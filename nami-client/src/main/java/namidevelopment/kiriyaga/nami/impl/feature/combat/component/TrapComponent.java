package namidevelopment.kiriyaga.nami.impl.feature.combat.component;

import namidevelopment.kiriyaga.api.core.breakprediction.PlayerBreakState;
import namidevelopment.kiriyaga.api.core.rotation.model.RotationRequest;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.event.impl.Render3DEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import namidevelopment.kiriyaga.api.util.BlockUtils;
import namidevelopment.kiriyaga.api.util.InteractionUtils;
import namidevelopment.kiriyaga.api.util.render.RenderUtil;
import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import com.mojang.blaze3d.vertex.PoseStack;
import namidevelopment.kiriyaga.nami.impl.feature.combat.autocrystal.AutoCrystalFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static namidevelopment.kiriyaga.api.NamiApi.*;
import static namidevelopment.kiriyaga.api.util.RotationUtils.*;

public class TrapComponent {

    public final DoubleSetting range;
    public final IntSetting delay;
    public final IntSetting shiftTicks;
    public final BoolSetting airPlace;
    public final BoolSetting grim;
    public final BoolSetting rotate;
    public final BoolSetting strictDirection;
    public final BoolSetting swapBack;
    public final BoolSetting multiTask;
    public final BoolSetting simulate;
    public final BoolSetting antiBreak;
    public final BoolSetting foundation;
    public final BoolSetting swing;
    public final BoolSetting render;
    public final BoolSetting attack;
    public final DoubleSetting attackRange;
    public final BoolSetting attackRotate;
    public final BoolSetting attackMultiTask;
    public final BoolSetting attackSwing;
    public final IntSetting attackAge;

    private int cooldown = 0;
    private final List<BlockPos> targetPositions = new ArrayList<>();
    private final List<BlockPos> placedPositions = new ArrayList<>();

    public TrapComponent(Feature feature) {
        range = feature.addSetting(new DoubleSetting("Range", 4.50, 1.0, 6.0));
        delay = feature.addSetting(new IntSetting("Delay", 0, 0, 5));
        shiftTicks = feature.addSetting(new IntSetting("ShiftTicks", 1, 1, 8));
        airPlace = feature.addSetting(new BoolSetting("AirPlace", false));
        grim = feature.addSetting(new BoolSetting("Grim", false));
        rotate = feature.addSetting(new BoolSetting("Rotate", true));
        strictDirection = feature.addSetting(new BoolSetting("StrictDirection", true));
        swapBack = feature.addSetting(new BoolSetting("SwapBack", true));
        multiTask = feature.addSetting(new BoolSetting("MultiTask", false));
        simulate = feature.addSetting(new BoolSetting("Simulate", false));
        antiBreak = feature.addSetting(new BoolSetting("AntiBreak", false));
        foundation = feature.addSetting(new BoolSetting("Foundation", false));
        swing = feature.addSetting(new BoolSetting("Swing", true));
        render = feature.addSetting(new BoolSetting("Render", true));

        attack = feature.addSetting(new BoolSetting("Attack", false));
        attackRotate = feature.addSetting(new BoolSetting("AttackRotate","Rotate", true));
        attackRange = feature.addSetting(new DoubleSetting("AttackRange","Range", 3.00, 1.0, 6.0));
        attackAge = feature.addSetting(new IntSetting("Age", 5, 0, 20));
        attackMultiTask = feature.addSetting(new BoolSetting("AttackMultitask","Multitask", true));
        attackSwing = feature.addSetting(new BoolSetting("AttackSwing","Swing", true));


        grim.setShowCondition(airPlace::get);
        strictDirection.setShowCondition(() -> !airPlace.get());

        attackRotate.setShowCondition(attack::get);
        attackRange.setShowCondition(attack::get);
        attackMultiTask.setShowCondition(attack::get);
        attackSwing.setShowCondition(attack::get);
        attackAge.setShowCondition(attack::get);
    }

    public void onDisable() {
        cooldown = 0;
        targetPositions.clear();
        placedPositions.clear();
    }

    public List<BlockPos> getTargetPositions() {
        return targetPositions;
    }

    public void onTick(PreTickEvent event, Feature owner, List<BlockPos> newTargets) {
        if (MC.player == null || MC.level == null) return;

        if (simulate.get() && !placedPositions.isEmpty()) {
            Item handItem = MC.player.getMainHandItem().getItem();

            for (BlockPos pos : placedPositions) {
                InteractionUtils.interactBlockAt(pos, handItem, null, swapBack.get(), multiTask.get(), range.get(), rotate.get(), strictDirection.get(), false, swing.get(), owner.getName()+"_interact");
            }

            placedPositions.clear();
        }

        targetPositions.clear();
        if (newTargets != null) targetPositions.addAll(newTargets);

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        if (attack.get() && !targetPositions.isEmpty()) {
            for (EndCrystal crystal : MC.level.getEntitiesOfClass(EndCrystal.class, new AABB(MC.player.blockPosition()).inflate(range.get() + 6.0))) {
                AABB crystalBox = crystal.getBoundingBox();
                for (BlockPos pos : targetPositions) {
                    AABB blockBox = new AABB(pos);
                    if (blockBox.intersects(crystalBox)) {
                        if (crystal.tickCount >= attackAge.get())
                            doBreak(crystal, owner);
                        break;
                    }
                }
            }
        }

        if (antiBreak.get() && !targetPositions.isEmpty()) {
            List<BlockPos> extraTargets = new ArrayList<>();
            for (BlockPos pos : targetPositions) {
                boolean breaking = false;
                for (PlayerBreakState state : BREAKPREDICT_SERVICE.all()) {
                    if (state == null) continue;

                    if (state.isBreaking(pos)) {
                        breaking = true;
                        break;
                    }
                }

                if (!breaking) continue;

                for (Direction dir : Direction.values()) {
                    if (dir == Direction.DOWN) continue;

                    BlockPos around = pos.relative(dir);

                    if (targetPositions.contains(around)) continue;
                    if (extraTargets.contains(around)) continue;

                    extraTargets.add(around);
                }
            }

            targetPositions.addAll(extraTargets);
        }

        int blocksPlaced = 0;

        for (BlockPos pos : targetPositions) {

            if (foundation.get()) {
                BlockPos foundation = pos.below();
                if (place(foundation, getSlot(), airPlace.get(), grim.get(), owner)) {
                    blocksPlaced++;
                    if (blocksPlaced >= shiftTicks.get()) break;
                }
            }

            if (place(pos, getSlot(), airPlace.get(), grim.get(), owner)) {
                placedPositions.add(pos);
                blocksPlaced++;
                if (blocksPlaced >= shiftTicks.get()) break;
            }
        }

        if (blocksPlaced > 0) {
            cooldown = delay.get();
        }
    }

    public void onRender(Render3DEvent event) {
        if (MC.player == null || MC.level == null) return;
        if (!render.get()) return;
        if (targetPositions.isEmpty()) return;

        ColorFeature colorFeature = FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class);
        Color color = colorFeature.getStyledGlobalColor();

        for (BlockPos pos : targetPositions) {
            if (!MC.level.getBlockState(pos).canBeReplaced())
                continue;

            AABB box = new AABB(pos);
            RenderUtil.drawBoxLines(box, color, true, true, 1.5f);
        }
    }

    private Item getSlot() {
        if (MC.player == null) return null;

        if (MC.player.getOffhandItem().getItem() instanceof BlockItem b) {
            if (b.getBlock().getExplosionResistance() >= 600.0f)
                return MC.player.getOffhandItem().getItem();
        }

        if (MC.player.getMainHandItem().getItem() instanceof BlockItem b) {
            if (b.getBlock().getExplosionResistance() >= 600.0f)
                return MC.player.getMainHandItem().getItem();
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = MC.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            Item item = stack.getItem();
            if (item instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                if (block.getExplosionResistance() >= 600.0f) {
                    return item;
                }
            }
        }

        return null;
    }

    private boolean place(BlockPos pos, Item item, boolean airPlace, boolean grim, Feature owner) {
        if (airPlace)
            return InteractionUtils.airPlace(pos, Direction.DOWN, item, swapBack.get(), range.get(), rotate.get(), grim, simulate.get(), swing.get(), owner.getName()+"_airplace", multiTask.get());

        return InteractionUtils.placeBlock(pos, item, swapBack.get(), range.get(), rotate.get(), strictDirection.get(), simulate.get(), swing.get(), owner.getName()+"_place", multiTask.get());
    }

    private void doBreak(EndCrystal target, Feature owner) {
        if (target == null) return;

        if (!attackMultiTask.get() && MC.player.isUsingItem()) return;

        boolean rotated = false;

        if (attackRotate.get()) {
            Vec3 pos = getClosestPointToEye(MC.player.getEyePosition(), target.getBoundingBox());
            float yaw = (float) getYawToVec(MC.player, pos);
            float pitch = (float) getPitchToVec(MC.player, pos);

            ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(owner.getName()+"_attack", 9, yaw, pitch));

            rotated = true;
        }

        if (rotated) {
            boolean insideBox = target.getBoundingBox().contains(MC.player.getEyePosition(1.0f));
            EntityHitResult serverCheck = raycastTarget(MC.player, target, attackRange.get(), ROTATION_SERVICE.getStateHandler().getServerYaw(), ROTATION_SERVICE.getStateHandler().getServerPitch());
            if (serverCheck == null && !insideBox) return;
        }

        MC.gameMode.attack(MC.player, target);

        if (attackSwing.get()) {
            MC.player.swing(InteractionHand.MAIN_HAND);
        }
    }
}
