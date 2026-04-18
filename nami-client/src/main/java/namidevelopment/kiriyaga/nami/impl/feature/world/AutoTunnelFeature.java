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
import namidevelopment.kiriyaga.api.util.InteractionUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;
@RegisterFeature
public class AutoTunnelFeature extends Feature {

    public enum TunnelMode {
        P1x1,
        P1x2,
        P1x3,
        P3x3
    }

    public final EnumSetting<TunnelMode> mode = addSetting(new EnumSetting<>("Mode", TunnelMode.P1x2));
    public final DoubleSetting distance = addSetting(new DoubleSetting("Range", 5.0, 1.0, 6.0));
    public final BoolSetting rotate = addSetting(new BoolSetting("Rotate", true));
    public final BoolSetting swing = addSetting(new BoolSetting("Swing", true));
    public final BoolSetting grim = addSetting(new BoolSetting("Grim", false));
    public final BoolSetting strictDirection = addSetting(new BoolSetting("StrictDirection", true));

    public AutoTunnelFeature() {
        super("AutoTunnel", "Automatically tunnels blocks in front of you.", FeatureCategory.of("World"));
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onPreTickEvent(PreTickEvent event) {
        if (MC.player == null || MC.level == null) return;

        BlockPos playerPos = MC.player.blockPosition();
        Set<BlockPos> validTargets = new HashSet<>();

        double maxDistance = distance.get();
        int maxDepth = (int) Math.ceil(maxDistance);

        for (int depth = 1; depth <= maxDepth; depth++) {
            BlockPos forward = playerPos.relative(MC.player.getDirection(), depth);

            switch (mode.get()) {
                case P1x1 -> addBlockIfBreakable(validTargets, forward, maxDistance);
                case P1x2 -> {
                    addBlockIfBreakable(validTargets, forward, maxDistance);
                    addBlockIfBreakable(validTargets, forward.above(), maxDistance);
                }
                case P1x3 -> {
                    addBlockIfBreakable(validTargets, forward, maxDistance);
                    addBlockIfBreakable(validTargets, forward.above(), maxDistance);
                    addBlockIfBreakable(validTargets, forward.above(2), maxDistance);
                }
                case P3x3 -> {
                    Direction right = MC.player.getDirection().getClockWise();
                    for (int side = -1; side <= 1; side++) {
                        for (int y = 0; y <= 2; y++) {
                            BlockPos checkPos = forward.relative(right, side).above(y);
                            addBlockIfBreakable(validTargets, checkPos, maxDistance);
                        }
                    }
                }
            }
        }

        BlockPos bestTarget = validTargets.stream()
                .min(Comparator.comparingDouble(a -> MC.player.distanceToSqr(
                        a.getX() + 0.5, a.getY() + 0.5, a.getZ() + 0.5)))
                .orElse(null);

        if (bestTarget != null) {
            InteractionUtils.breakBlock(
                    bestTarget,
                    distance.get(),
                    rotate.get(),
                    swing.get(),
                    grim.get(),
                    strictDirection.get(),
                    this.name
            );
        }
    }

    private void addBlockIfBreakable(Set<BlockPos> set, BlockPos pos, double maxDistance) {
        BlockState state = MC.level.getBlockState(pos);

        if (state.isAir()) return;
        if (!state.getFluidState().isEmpty()) return;
        if (state.getDestroySpeed(MC.level, pos) < 0) return;

        Vec3 eyePos = MC.player.getEyePosition();
        double cx = Math.clamp(eyePos.x, pos.getX(), pos.getX() + 1.0);
        double cy = Math.clamp(eyePos.y, pos.getY(), pos.getY() + 1.0);
        double cz = Math.clamp(eyePos.z, pos.getZ(), pos.getZ() + 1.0);
        if (eyePos.distanceTo(new Vec3(cx, cy, cz)) > maxDistance) return;

        set.add(pos);
    }
}
