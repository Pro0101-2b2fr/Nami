package namidevelopment.kiriyaga.api.core.rotation;

import namidevelopment.kiriyaga.api.contract.FeatureContractService;
import namidevelopment.kiriyaga.api.contract.feature.RotationsFeatureConfig;
import namidevelopment.kiriyaga.api.core.rotation.model.RotationRequest;
import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.util.InputCache;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;

import static namidevelopment.kiriyaga.api.NamiApi.*;
import static namidevelopment.kiriyaga.api.util.RotationUtils.*;


public class RotationTickHandler {

    private final RotationStateHandler stateHandler;
    private final RotationRequestHandler requestHandler;

    private float currentYawSpeed = 0f, currentPitchSpeed = 0f;
    private int ticksHolding = 0;
    private boolean returning = false;

    public RotationTickHandler(RotationStateHandler stateHandler, RotationRequestHandler requestHandler) {
        this.stateHandler = stateHandler;
        this.requestHandler = requestHandler;
    }

    public void init() {
        EVENT_SERVICE.register(this);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreTick(PreTickEvent event) {
        if (MC.player == null) return;
        RotationsFeatureConfig rotationsFeatureConfig = FeatureContractService.get(RotationsFeatureConfig.class);

        stateHandler.updateRealRotation(MC.player.getYRot(), MC.player.getXRot());

        RotationRequest active = requestHandler.getActiveRequest();
        if (rotationsFeatureConfig.getRotationMode() == RotationsFeatureConfig.RotationMode.SILENT && stateHandler.getSilentSyncRequired()) {
            //performSilent(active); // actually this can be skipped if we somehow simulate client rotation packet sending idk
            //stateHandler.setSilentSyncRequired(false);
            //resetRotationToReal();
            requestHandler.clear();
            returning = false;
            return;
        }

        if (active != null) {
            processRequest(active);
        } else if (returning) {
            returnToRealRotation();
        } else {
            idleReset();
        }

        if (rotationsFeatureConfig.isMoveFixEnabled() && stateHandler.isRotating())
            fixMovementForSpoof();
    }

    private void fixMovementForSpoof() {
        if (MC.player == null || INPUT_SERVICE.isFrozen()) return;

        float realYaw = MC.player.getYRot();
        float spoofYaw = stateHandler.getRotationYaw();
        float delta = Mth.wrapDegrees(realYaw - spoofYaw);

        // theese are tick thread and render thread
        boolean forward = INPUT_SERVICE.isForwardPressed();
        boolean back = INPUT_SERVICE.isBackPressed();
        boolean left = INPUT_SERVICE.isLeftPressed();
        boolean right = INPUT_SERVICE.isRightPressed();

        InputCache.update(
                forward,
                back,
                left,
                right
        );

        float inputX = (right ? 1 : 0) - (left ? 1 : 0);
        float inputZ = (forward ? 1 : 0) - (back ? 1 : 0);

        MC.options.keyUp.setDown(false);
        MC.options.keyDown.setDown(false);
        MC.options.keyLeft.setDown(false);
        MC.options.keyRight.setDown(false);

        if (inputX == 0 && inputZ == 0) return;

        double moveAngle = Math.toDegrees(Math.atan2(inputX, inputZ));
        double finalAngle = moveAngle + delta;
        int sector = (int) Math.round(finalAngle / 45.0) & 7;

        // i hate myself its 02:28
        switch (sector) {
            case 0: MC.options.keyUp.setDown(true); break;
            case 1: MC.options.keyUp.setDown(true); MC.options.keyRight.setDown(true); break;
            case 2: MC.options.keyRight.setDown(true); break;
            case 3: MC.options.keyDown.setDown(true); MC.options.keyRight.setDown(true); break;
            case 4: MC.options.keyDown.setDown(true); break;
            case 5: MC.options.keyDown.setDown(true); MC.options.keyLeft.setDown(true); break;
            case 6: MC.options.keyLeft.setDown(true); break;
            case 7: MC.options.keyUp.setDown(true); MC.options.keyLeft.setDown(true); break;
        }
    }

    private void processRequest(RotationRequest request) {
        RotationsFeatureConfig rotationsFeatureConfig = FeatureContractService.get(RotationsFeatureConfig.class);

        if (!request.id.equals(requestHandler.getLastActiveId())) {
            resetRotationToReal();
            requestHandler.setLastActiveId(request.id);
        }

        boolean updated = false;
        if (request.shouldUpdate()) {
            float oldYaw = request.targetYaw;
            float oldPitch = request.targetPitch;
            request.updateTarget();
            updated = Math.abs(oldYaw - request.targetYaw) > 0.001f || Math.abs(oldPitch - request.targetPitch) > 0.001f;
            if (updated) ticksHolding = 0;
        }

        float yawDiff = yawDifference(request.targetYaw, stateHandler.getRotationYaw());
        float pitchDiff = request.targetPitch - stateHandler.getRotationPitch();

        boolean reached = Math.abs(yawDiff) <= rotationsFeatureConfig.getRotationThreshold() && Math.abs(pitchDiff) <= rotationsFeatureConfig.getRotationThreshold();

        if (reached && !updated) {
            if (++ticksHolding >= rotationsFeatureConfig.getHoldTicks()) {
                requestHandler.removeActiveRequest();
                ticksHolding = 0;
                returning = true;
            }
        } else {
            ticksHolding = 0;
            interpolateRotation(yawDiff, pitchDiff);
        }
    }

    // in resetRotationToReal() returnToRealRotation() we need to set player yaw, clamped to closest to rotation yaw
    // we need to do this between rotation requests change (highest priority appeared when old one not finished)
    // and when rotation is ended
    // this is made to prevent yaw jump
    private void resetRotationToReal() {
        float targetYaw = alignYaw(stateHandler.getRealYaw(), stateHandler.getRotationYaw());
        stateHandler.updateRealRotation(targetYaw, stateHandler.getRealPitch());
        MC.player.setYRot(targetYaw);
        stateHandler.setRotationYaw(stateHandler.getRealYaw());
        stateHandler.setRotationPitch(stateHandler.getRealPitch());
        currentYawSpeed = 0f;
        currentPitchSpeed = 0f;
        ticksHolding = 0;
        returning = false;
    }

    private void returnToRealRotation() {
        RotationsFeatureConfig rotationsFeatureConfig = FeatureContractService.get(RotationsFeatureConfig.class);
        float targetYaw = alignYaw(stateHandler.getRealYaw(), stateHandler.getRotationYaw());
        float yawDiff = targetYaw - stateHandler.getRotationYaw();
        float pitchDiff = stateHandler.getRealPitch() - stateHandler.getRotationPitch();

        interpolateRotation(yawDiff, pitchDiff);

        boolean backReached = Math.abs(yawDiff) <= rotationsFeatureConfig.getRotationThreshold() && Math.abs(pitchDiff) <= rotationsFeatureConfig.getRotationThreshold();
        if (backReached) {
            returning = false;
            stateHandler.updateRealRotation(targetYaw, stateHandler.getRealPitch());
            MC.player.setYRot(targetYaw);
            stateHandler.setRotationYaw(stateHandler.getRealYaw());
            stateHandler.setRotationPitch(stateHandler.getRealPitch());
            requestHandler.clearLastActiveId();

/*            if (Feature_SERVICE.getStorage().getByClass(RotationFeature.class).rotation.get() == RotationFeature.RotationMode.MOTION) {
                SprintFeature sm = Feature_SERVICE.getStorage().getByClass(SprintFeature.class);

                if (sm.twobtwot.get())
                    sm.stopSprinting(2);
            }*/
        }
    }

    private void interpolateRotation(float yawDiff, float pitchDiff) {
        RotationsFeatureConfig rotationsFeatureConfig = FeatureContractService.get(RotationsFeatureConfig.class);

        currentYawSpeed = lerp(currentYawSpeed, yawDiff, (float) rotationsFeatureConfig.getRotationEase());
        currentPitchSpeed = lerp(currentPitchSpeed, pitchDiff, (float) rotationsFeatureConfig.getRotationEase());

        float yawSpeed = (float) Mth.clamp(currentYawSpeed, -rotationsFeatureConfig.getRotationSpeed(), rotationsFeatureConfig.getRotationSpeed());
        float pitchSpeed = (float) Mth.clamp(currentPitchSpeed, -rotationsFeatureConfig.getRotationSpeed(), rotationsFeatureConfig.getRotationSpeed());

        float newYaw = stateHandler.getRotationYaw() + yawSpeed;
        float newPitch = stateHandler.getRotationPitch() + pitchSpeed;

        if (rotationsFeatureConfig.getJitterMode() == RotationsFeatureConfig.JitterMode.NORMAL) {
            float minJitter = (float) (rotationsFeatureConfig.getRotationThreshold() / 4f);
            float maxJitter = (float) (rotationsFeatureConfig.getRotationThreshold() / 2);
            float jitterYaw = minJitter + (float) (Math.random() * (maxJitter - minJitter));
            float jitterPitch = minJitter + (float) (Math.random() * (maxJitter - minJitter));
            jitterYaw *= Math.random() < 0.5 ? -1 : 1;
            jitterPitch *= Math.random() < 0.5 ? -1 : 1;

            newYaw += jitterYaw;
            newPitch += jitterPitch;

            newPitch = Mth.clamp(newPitch, -90f, 90f);
        } else if (rotationsFeatureConfig.getJitterMode() == RotationsFeatureConfig.JitterMode.GRIM) {
            float f = (float)((Math.random() * 2.0 - 1.0) * 0.001f);
            newPitch = Mth.clamp(newPitch + f, -90.0F, 90.0F);
        }

//        RotationSERVICEFeature Feature = Feature_SERVICE.getStorage().getByClass(RotationSERVICEFeature.class);
//        if (Feature.mouseDeltaFix.get()) { // https://github.com/GrimAnticheat/Grim/blob/57a9f8f432800382d43c28df9e8409b4d7d80813/common/src/main/java/ac/grim/grimac/checks/impl/aim/AimModulo360.java#L31
//            float lastServerYaw = stateHandler.getServerYaw();
//            float lastServerDeltaYaw = stateHandler.getServerDeltaYaw();
//
//            float rawDiff = yawDifference(newYaw, lastServerYaw);
//
//            Feature_SERVICE.getStorage().getByClass(Debug.class).debugDelta(Text.of(
//                    "before wrap: newYaw=" + newYaw +
//                            ", lastServerYaw=" + lastServerYaw +
//                            ", lastServerDeltaYaw=" + lastServerDeltaYaw +
//                            ", rawDiff=" + rawDiff
//            ));
//
//            while (rawDiff > 360f) rawDiff -= 360f;
//            while (rawDiff < -360f) rawDiff += 360f;
//
//            Feature_SERVICE.getStorage().getByClass(Debug.class).debugDelta(Text.of(
//                    "After wrap: rawDiff=" + rawDiff
//            ));
//
//            if (Math.abs(rawDiff) > 320f && Math.abs(lastServerDeltaYaw) < 30f) {
//                newYaw = lastServerYaw + Math.copySign(319f, rawDiff);
//                Feature_SERVICE.getStorage().getByClass(Debug.class).debugDelta(Text.of(
//                        "clipping applied: newYaw before=" + newYaw +
//                                ", newYaw after=" + newYaw +
//                                ", clipSign=" + Math.copySign(1f, rawDiff)
//                ));
//            }
//
//
//            Feature_SERVICE.getStorage().getByClass(Debug.class).debugDelta(Text.of(
//                    "Final newYaw=" + newYaw
//            ));
//        }

        stateHandler.setRotationYaw(newYaw);
        stateHandler.setRotationPitch(newPitch);
    }

    private float lerpAngle(float start, float end, float factor) {
        float delta = wrapDegrees(end - start);
        return start + delta * factor;
    }

    private void idleReset() {
        requestHandler.clearLastActiveId();
        stateHandler.setRotationYaw(stateHandler.getRealYaw());
        stateHandler.setRotationPitch(stateHandler.getRealPitch());
        currentYawSpeed = 0f;
        currentPitchSpeed = 0f;
    }

    private void performSilent(RotationRequest req) {
        float targetYaw = MC.player.getYRot();
        float targetPitch = MC.player.getXRot();
        // AimModulo360 seems fixable here but due to race condition it fucks a little bit screen, maybe ill fix it someday but now we just left it with flag
//        ROTATION_SERVICE.getStateHandler().setRotationYaw(targetYaw);
//        ROTATION_SERVICE.getStateHandler().setRotationPitch(targetPitch);
        ROTATION_SERVICE.getStateHandler().setServerYaw(targetYaw);
        ROTATION_SERVICE.getStateHandler().setServerPitch(targetPitch);
//
//        returning = false;
//        stateHandler.updateRealRotation(targetYaw, stateHandler.getRealPitch());
//        MC.player.setYaw(targetYaw);
//        requestHandler.clearLastActiveId();
//        requestHandler.removeActiveRequest();

        MC.getConnection().send(new ServerboundMovePlayerPacket.PosRot(MC.player.getX(), MC.player.getY(), MC.player.getZ(), targetYaw, targetPitch, MC.player.onGround(), true));
    }

    private float lerp(float from, float to, float factor) {
        return from + (to - from) * factor;
    }

    public boolean isRotating() {
        return requestHandler.getActiveRequest() != null || returning;
    }
}
