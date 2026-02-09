package namidevelopment.kiriyaga.nami.impl.feature.combat.autocrystal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

public record AutoCrystalSnapshot(long tickId, int selfId, Vec3 eyePos, BlockPos playerBlockPos, double placeRange, double breakRange, double minDamage, boolean assumeBestArmor, boolean facePlace, double facePlaceHealth, TargetData[] targets, BlockPos[] candidatePos) {
    public record TargetData(
            int id,
            Vec3 pos,
            AABB box,
            float armor,
            float toughness,
            int resistanceAmp, //-1 if none is present
            byte armorMask, // 1 helmet, 2 chestplate , 4 leggins ,8 boots
            int prot,
            int blastProt,
            float health,
            float absorption
    ) {

    }

    public static final class AsyncDebugInfo {
        final long tickId;
        final long startNs = System.nanoTime();

        int targetsTotal;
        int targetsValid;

        int candidatesTotal;
        int candidatesBlocked;
        int candidatesBadBase;
        int candidatesNotAir;
        int candidatesOutPlaceRange;
        int candidatesOutBreakRange;

        int dmgRejectedMin;
        int dmgRejectedSelf;
        int dmgRejectedNoSelfPop;
        int bestFound;

        AsyncDebugInfo(long tickId) {
            this.tickId = tickId;}

        String buildMessage(float totalMs) {
            return String.format(Locale.US, "AsyncCalc tick=%d time=%.3fms | targets=%d/%d | cand=%d (blocked=%d base=%d air=%d pr=%d br=%d) | rej(min=%d self=%d pop=%d) | best=%d", tickId, totalMs, targetsValid, targetsTotal, candidatesTotal, candidatesBlocked, candidatesBadBase, candidatesNotAir, candidatesOutPlaceRange, candidatesOutBreakRange, dmgRejectedMin, dmgRejectedSelf, dmgRejectedNoSelfPop, bestFound);
        }
    }

}
