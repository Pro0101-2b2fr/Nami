package namidevelopment.kiriyaga.api.core.breakprediction;

import namidevelopment.kiriyaga.api.event.impl.Render3DEvent;
import namidevelopment.kiriyaga.api.util.render.RenderUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.awt.*;
import java.util.*;
import java.util.List;

import static namidevelopment.kiriyaga.api.NamiApi.*;

public class PlayerBreakState {

    private final UUID playerId;
    private final BreakTask current;
    private final BreakTask doubleMine;

    private final List<BlockPos> brokenBlocks = new ArrayList<>();

    public PlayerBreakState(UUID uuid) {
        this.playerId = uuid;
        this.current = new BreakTask(uuid);
        this.doubleMine = new BreakTask(uuid);
    }

    public Player getPlayer() {
        return MC.level.getPlayerByUUID(playerId);
    }

    public void onTick() {
        current.onTick();
        doubleMine.onTick();
    }

    public void render(Render3DEvent event, boolean fill) {
        if (current.isActive()) renderProgress(event, current, fill);
        if (doubleMine.isActive()) renderProgress(event, doubleMine, fill);
    }

    private void renderProgress(Render3DEvent event, BreakTask task, boolean fill) {
        BlockPos pos = task.getBlockPos();
        BlockState state = task.getBlockState();

        if (state.isAir()) return;

        VoxelShape shape = task.isInstant() ? Shapes.block() : state.getShape(MC.level, pos);
        if (shape.isEmpty()) shape = Shapes.block();

        AABB bb = shape.bounds();
        AABB worldBox = new AABB(pos.getX() + bb.minX, pos.getY() + bb.minY, pos.getZ() + bb.minZ, pos.getX() + bb.maxX, pos.getY() + bb.maxY, pos.getZ() + bb.maxZ);
        Vec3 center = worldBox.getCenter();
        float partialTicks = event.getTickDelta();
        float interpolated = task.getPreviousProgress() + (task.getProgress() - task.getPreviousProgress()) * partialTicks;
        float scale = Mth.clamp(interpolated / task.getTargetSpeed(), 0, 1);
        double dx = (bb.maxX - bb.minX) / 2;
        double dy = (bb.maxY - bb.minY) / 2;
        double dz = (bb.maxZ - bb.minZ) / 2;

        AABB box = new AABB(center, center).inflate(dx * scale, dy * scale, dz * scale);

        RenderUtil.drawBoxLines(box, new Color(200, 150, 0, 255), fill, true, 1.5f);
    }

    public void startBreak(BlockPos pos, Direction face, float speed) {
        if (current.isActive() && !current.pos.equals(pos)) {
                if (!doubleMine.isActive()) {
                    doubleMine.copyFrom(current);
                    doubleMine.setTargetSpeed(1.0f);
                }
                current.reset();
            }

        if (!current.isActive() || !current.pos.equals(pos)) {
            current.start(pos, face, speed);
        }
    }

    public void abortCurrent() {
        current.reset();
    }

    public void finishCurrent() {
        if (!current.isActive()) return;
        brokenBlocks.add(current.getBlockPos());
        current.reset();
    }

    public static class BreakTask {

        private final UUID playerId;

        private BlockPos pos;
        private Direction face;
        private float targetSpeed;

        private float progress;
        private float prevProgress;
        private boolean instant;

        private boolean active;

        public BreakTask(UUID playerId) {
            this.playerId = playerId;
        }

        public void setTargetSpeed(float speed) {
            this.targetSpeed = speed;
        }

        public void start(BlockPos pos, Direction face, float speed) {
            this.pos = pos;
            this.face = face;
            this.targetSpeed = speed;
            this.progress = 0;
            this.prevProgress = 0;
            this.instant = false;
            this.active = true;
        }

        public void copyFrom(BreakTask other) {
            this.pos = other.pos;
            this.face = other.face;
            this.targetSpeed = other.targetSpeed;
            this.progress = other.progress;
            this.prevProgress = other.prevProgress;
            this.instant = other.instant;
            this.active = other.active;
        }

        public void reset() {
            this.active = false;
            this.progress = 0;
            this.prevProgress = 0;
            this.instant = false;
        }

        public void onTick() {
            if (!active || MC.level == null) return;

            Player player = MC.level.getPlayerByUUID(playerId);
            if (player == null) {
                reset();
                return;
            }

            BlockState state = getBlockState();
            if (state.isAir()) {
                instant = true;
                return;
            }

            float delta = calculateBlockDamage(state, MC.level, pos, player);
            prevProgress = progress;
            progress += delta;

            if (progress >= targetSpeed || instant) {
                active = false;
            }
        }

        public boolean isActive() {
            return active;
        }

        public BlockPos getBlockPos() {
            return pos;
        }

        public BlockState getBlockState() {
            return MC.level.getBlockState(pos);
        }

        public float getTargetSpeed() {
            return targetSpeed;
        }

        public float getProgress() {
            return progress;
        }

        public float getPreviousProgress() {
            return prevProgress;
        }

        public boolean isInstant() {
            return instant;
        }

        private float calculateBlockDamage(BlockState state, BlockGetter world, BlockPos pos, Player player) {
            float hardness = state.getDestroySpeed(world, pos);
            if (hardness == -1.0f) return 0.0f;

            int divisor = 30;
            float speed = getMiningSpeed(state, player);

            return speed / hardness / divisor;
        }

        private float getMiningSpeed(BlockState state, Player player) {
            ItemStack stack = new ItemStack(Items.NETHERITE_PICKAXE);
            float speed = stack.getDestroySpeed(state);

            if (speed > 1.0f) {
                //int level = EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.EFFICIENCY);
                int level = 5;
                if (level > 0 && !stack.isEmpty()) {
                    speed += (level * level + 1);
                }
            }

/*            if (MobEffectUtil.hasDigSpeed(player)) {
                int amplifier = MobEffectUtil.getDigSpeedAmplification(player) + 1;
                speed *= 1.0f + amplifier * 0.2f;
            }*/

           // boolean noAquaAffinity = EnchantmentUtils.getEnchantmentLevel(player.getItemBySlot(EquipmentSlot.HEAD), Enchantments.AQUA_AFFINITY) == 0;
           // boolean noAquaAffinity = !player.onGround();
            //if (player.isEyeInFluid(FluidTags.WATER) && noAquaAffinity) {
              //  speed /= 5.0f;
            //}

            if (!player.onGround()) {
                speed /= 5.0f;
            }

            return speed;
        }
    }

    public boolean isInBreakProgress(BlockPos pos) {
        if (current.isActive() && current.getBlockPos().equals(pos)) return true;
        if (doubleMine.isActive() && doubleMine.getBlockPos().equals(pos)) return true;
        return false;
    }

    public boolean isBreaking(BlockPos pos) {
        if (current.getBlockPos() != null && current.getBlockPos().equals(pos) && current.isActive())
            return true;

        return doubleMine.getBlockPos() != null && doubleMine.getBlockPos().equals(pos) && doubleMine.isActive();
    }
}
