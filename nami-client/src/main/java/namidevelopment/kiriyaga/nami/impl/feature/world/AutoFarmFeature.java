package namidevelopment.kiriyaga.nami.impl.feature.world;

import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import namidevelopment.kiriyaga.api.util.InteractionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;

@RegisterFeature
public class AutoFarmFeature extends Feature {

    public enum FarmMode {
        CROPS, // Wheat, Carrots, Potatoes, Beetroot, etc. on Farmland
        NETHER_WART, // Nether Wart on Soul Sand
        SUGAR_CANE, // Break sugar cane above the base block
        BAMBOO, // Break bamboo above the base block
        CACTUS, // Break cactus above the base block
        COCOA, // Harvest mature Cocoa Beans on Jungle Logs
        ALL // Harvest and replant everything at once
    }

    public final EnumSetting<FarmMode> mode = addSetting(new EnumSetting<>("Mode", FarmMode.CROPS));
    public final DoubleSetting range = addSetting(new DoubleSetting("Range", 4.5, 1.0, 6.0));
    public final IntSetting radius = addSetting(new IntSetting("Radius", 6, 1, 8));
    public final BoolSetting rotate = addSetting(new BoolSetting("Rotate", true));
    public final BoolSetting swapSilent = addSetting(new BoolSetting("SwapSilent", true));
    public final BoolSetting swing = addSetting(new BoolSetting("Swing", true));
    public final BoolSetting simulate = addSetting(new BoolSetting("Simulate", false));
    public final BoolSetting strictDirection = addSetting(new BoolSetting("StrictDirection", true));
    public final BoolSetting multiTask = addSetting(new BoolSetting("MultiTask", false));

    public AutoFarmFeature() {
        super("AutoFarm", "Automatically harvests and replants crops.", FeatureCategory.of("World"));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPreTickEvent(PreTickEvent e) {
        if (MC.player == null || MC.level == null)
            return;

        BlockPos playerPos = MC.player.blockPosition();
        int r = radius.get();
        double maxRange = range.get();

        Set<BlockPos> harvestTargets = new HashSet<>();
        Set<BlockPos> plantTargets = new HashSet<>();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);

                    if (!isInRange(pos, maxRange))
                        continue;

                    switch (mode.get()) {
                        case CROPS -> {
                            if (isHarvestableCrop(pos))
                                harvestTargets.add(pos);
                            else if (isPlantableFarmland(pos))
                                plantTargets.add(pos);
                        }
                        case NETHER_WART -> {
                            if (isHarvestableNetherWart(pos))
                                harvestTargets.add(pos);
                            else if (isPlantableSoulSand(pos))
                                plantTargets.add(pos);
                        }
                        case SUGAR_CANE -> {
                            if (isHarvestableStem(pos, Blocks.SUGAR_CANE))
                                harvestTargets.add(pos);
                        }
                        case BAMBOO -> {
                            if (isHarvestableStem(pos, Blocks.BAMBOO))
                                harvestTargets.add(pos);
                        }
                        case CACTUS -> {
                            if (isHarvestableStem(pos, Blocks.CACTUS))
                                harvestTargets.add(pos);
                        }
                        case COCOA -> {
                            if (isHarvestableCocoa(pos))
                                harvestTargets.add(pos);
                        }
                        case ALL -> {
                            if (isHarvestableCrop(pos))
                                harvestTargets.add(pos);
                            else if (isHarvestableNetherWart(pos))
                                harvestTargets.add(pos);
                            else if (isHarvestableStem(pos, Blocks.SUGAR_CANE))
                                harvestTargets.add(pos);
                            else if (isHarvestableStem(pos, Blocks.BAMBOO))
                                harvestTargets.add(pos);
                            else if (isHarvestableStem(pos, Blocks.CACTUS))
                                harvestTargets.add(pos);
                            else if (isHarvestableCocoa(pos))
                                harvestTargets.add(pos);
                            else if (isPlantableFarmland(pos))
                                plantTargets.add(pos);
                            else if (isPlantableSoulSand(pos))
                                plantTargets.add(pos);
                        }
                    }
                }
            }
        }

        if (!harvestTargets.isEmpty()) {
            BlockPos best = closest(harvestTargets);
            if (best != null) {
                InteractionUtils.breakBlock(best, maxRange, rotate.get(), swing.get(), false, strictDirection.get(),
                        this.name);
            }
            return;
        }

        if (!plantTargets.isEmpty()) {
            BlockPos best = closest(plantTargets);
            if (best != null) {
                Item seed = getSeedForPos(best);
                if (seed != null) {
                    InteractionUtils.interactBlockAt(best, seed, Direction.UP, swapSilent.get(), multiTask.get(),
                            maxRange, rotate.get(), strictDirection.get(), simulate.get(), swing.get(), this.name);
                }
            }
        }
    }

    private boolean isInRange(BlockPos pos, double maxRange) {
        Vec3 eye = MC.player.getEyePosition();
        double cx = Math.clamp(eye.x, pos.getX(), pos.getX() + 1.0);
        double cy = Math.clamp(eye.y, pos.getY(), pos.getY() + 1.0);
        double cz = Math.clamp(eye.z, pos.getZ(), pos.getZ() + 1.0);
        return eye.distanceTo(new Vec3(cx, cy, cz)) <= maxRange;
    }

    /** True when a CropBlock is at its maximum age (ready to harvest). */
    private boolean isHarvestableCrop(BlockPos pos) {
        BlockState state = MC.level.getBlockState(pos);
        if (!(state.getBlock() instanceof CropBlock crop))
            return false;
        return crop.isMaxAge(state);
    }

    /** True when Nether Wart is fully grown (age == 3). */
    private boolean isHarvestableNetherWart(BlockPos pos) {
        BlockState state = MC.level.getBlockState(pos);
        if (!(state.getBlock() instanceof NetherWartBlock))
            return false;
        return state.getValue(NetherWartBlock.AGE) == 3;
    }

    /**
     * Sugar Cane / Bamboo / Cactus: break any block of the stem type
     * where the block directly below is also the same stem type.
     * This ensures the base block (on soil) is never broken and regrows naturally.
     */
    private boolean isHarvestableStem(BlockPos pos, Block stemBlock) {
        if (MC.level.getBlockState(pos).getBlock() != stemBlock)
            return false;
        return MC.level.getBlockState(pos.below()).getBlock() == stemBlock;
    }

    /** True when a Cocoa bean pod is fully grown (age == 2). */
    private boolean isHarvestableCocoa(BlockPos pos) {
        BlockState state = MC.level.getBlockState(pos);
        if (!(state.getBlock() instanceof CocoaBlock))
            return false;
        return state.getValue(CocoaBlock.AGE) == 2;
    }

    private boolean isPlantableFarmland(BlockPos pos) {
        if (MC.level.getBlockState(pos).getBlock() != Blocks.FARMLAND)
            return false;
        return MC.level.getBlockState(pos.above()).isAir();
    }

    private boolean isPlantableSoulSand(BlockPos pos) {
        if (MC.level.getBlockState(pos).getBlock() != Blocks.SOUL_SAND)
            return false;
        return MC.level.getBlockState(pos.above()).isAir();
    }

    private Item getSeedForPos(BlockPos pos) {
        Block base = MC.level.getBlockState(pos).getBlock();
        return base == Blocks.FARMLAND ? findFarmlandSeed() : findNetherWart();
    }

    private Item findFarmlandSeed() {
        ItemStack offhand = MC.player.getOffhandItem();
        if (!offhand.isEmpty() && isFarmlandSeed(offhand.getItem()))
            return offhand.getItem();
        for (int i = 0; i < 9; i++) {
            Item item = MC.player.getInventory().getItem(i).getItem();
            if (isFarmlandSeed(item))
                return item;
        }
        return null;
    }

    private Item findNetherWart() {
        ItemStack offhand = MC.player.getOffhandItem();
        if (!offhand.isEmpty() && offhand.getItem() == Items.NETHER_WART)
            return offhand.getItem();
        for (int i = 0; i < 9; i++) {
            Item item = MC.player.getInventory().getItem(i).getItem();
            if (item == Items.NETHER_WART)
                return item;
        }
        return null;
    }

    private boolean isFarmlandSeed(Item item) {
        if (item instanceof BlockItem bi && bi.getBlock() instanceof CropBlock)
            return true;
        return item == Items.CARROT
                || item == Items.POTATO
                || item == Items.BEETROOT_SEEDS
                || item == Items.MELON_SEEDS
                || item == Items.WHEAT_SEEDS
                || item == Items.PUMPKIN_SEEDS
                || item == Items.TORCHFLOWER_SEEDS
                || item == Items.PITCHER_POD;
    }

    private BlockPos closest(Set<BlockPos> set) {
        return set.stream()
                .min(Comparator.comparingDouble(a -> MC.player.distanceToSqr(
                        a.getX() + 0.5, a.getY() + 0.5, a.getZ() + 0.5)))
                .orElse(null);
    }
}
