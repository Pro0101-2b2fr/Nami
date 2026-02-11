package namidevelopment.kiriyaga.nami.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import namidevelopment.kiriyaga.api.event.impl.GlidingEvent;
import namidevelopment.kiriyaga.nami.impl.feature.client.RotationsFeature;
import namidevelopment.kiriyaga.nami.impl.feature.movement.HighJumpFeature;
import namidevelopment.kiriyaga.nami.impl.feature.exploits.NoJumpDelayFeature;
import namidevelopment.kiriyaga.nami.impl.feature.movement.NoLevitationFeature;
import namidevelopment.kiriyaga.nami.mixininterface.ILivingEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity implements ILivingEntity {

    @Shadow
    private int noJumpDelay;

    public MixinLivingEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Unique
    private boolean serverSideDead;

    @Override
    public void setServerSideDead(boolean value) {
        this.serverSideDead = value;
    }

    @Override
    public boolean isServerSideDead() {
        return serverSideDead;
    }
    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V", ordinal = 2, shift = At.Shift.BEFORE))
    private void doItemUse(CallbackInfo info) {
        NoJumpDelayFeature Feature = FEATURE_SERVICE.getStorage() != null ? FEATURE_SERVICE.getStorage().getByClass(NoJumpDelayFeature.class) : null;
        if (Feature != null && Feature.isEnabled()) {
            noJumpDelay = 0;
        }
    }

    @Inject(at = @At("HEAD"), method = "isFallFlying()Z", cancellable = true)
    private void isGlidingZ(CallbackInfoReturnable<Boolean> cir) {
        GlidingEvent ev = new GlidingEvent();

        EVENT_SERVICE.post(ev);

        if (ev.isCancelled())
            cir.setReturnValue(true);
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void onJump(CallbackInfo ci) {
        HighJumpFeature mod = FEATURE_SERVICE.getStorage() != null ? FEATURE_SERVICE.getStorage().getByClass(HighJumpFeature.class) : null;
        if (mod == null || !mod.isEnabled()) return;

        float jumpBoost = mod.height.get().floatValue();

        Vec3 vel = this.getDeltaMovement();
        this.setDeltaMovement(vel.x, Math.max(jumpBoost, vel.y), vel.z);

        if (this.isSprinting()) {
            float yawRad = this.getYRot() * 0.017453292F;
            this.addDeltaMovement(new Vec3(-Mth.sin(yawRad) * 0.2, 0.0, Mth.cos(yawRad) * 0.2));
        }

        this.needsSync = true;
        ci.cancel();
    }

//    @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
//    private void setSprinting(boolean sprinting, CallbackInfo ci) {
//        if ((Object)this != MinecraftClient.getInstance().player) return;
//
//        RotationSERVICEFeature rotationFeature = Feature_SERVICE.getStorage() != null ? Feature_SERVICE.getStorage().getByClass(RotationSERVICEFeature.class) : null;
//        if (rotationFeature == null || ROTATION_SERVICE == null || !ROTATION_SERVICE.getStateHandler().isRotating() || !rotationFeature.sprintFix.get())
//            return;
//
//        if (sprinting && MC.player.input != null) {
//            Vec2f movement = MC.player.input.getMovementInput();
//            float forward = movement.x;
//            float sideways = movement.y;
//
//            if (forward == 0 && sideways == 0) {
//                ci.cancel();
//                super.setSprinting(false);
//                return;
//            }
//
//            float spoofYaw = lastSendedYaw;
//            float realYaw = MC.player.getYaw();
//
//            Vec3d localMovement = new Vec3d(sideways, 0, forward);
//            Vec3d globalMovement = localToGlobal(localMovement, realYaw);
//
//            Vec3d localRelativeToSpoof = globalToLocal(globalMovement, spoofYaw);
//
//            double moveAngleRad = Math.atan2(localRelativeToSpoof.z, localRelativeToSpoof.x);
//            float moveAngleDeg = (float) Math.toDegrees(moveAngleRad);
//            moveAngleDeg = MathHelper.wrapDegrees(moveAngleDeg);
//
//            if (Math.abs(moveAngleDeg) > 33f) {
//                ci.cancel();
//                super.setSprinting(false);
//            }
//        }
//    }

    @ModifyReturnValue(method = "hasEffect", at = @At("RETURN"))
    private boolean hasStatusEffect(boolean original, Holder<MobEffect> effect) {
        if ((Object) this instanceof Player player &&
                player == MC.player) {

            NoLevitationFeature nl = FEATURE_SERVICE.getStorage().getByClass(NoLevitationFeature.class);
            if (nl != null && nl.isEnabled()) {
                ResourceKey<MobEffect> slowFallKey = MobEffects.SLOW_FALLING.unwrapKey().orElse(null);
                if (nl.noSlowFall.get() && slowFallKey != null && effect.is(slowFallKey))
                    return false;

                ResourceKey<MobEffect> levitationKey = MobEffects.LEVITATION.unwrapKey().orElse(null); // this just removes levitation damage reducing
                if (levitationKey != null && effect.is(levitationKey))
                    return false;
            }
        }
        return original;
    }

    @ModifyReturnValue(method = "getEffect", at = @At("RETURN"))
    private MobEffectInstance getStatusEffect(MobEffectInstance original, Holder<MobEffect> effect) {
        NoLevitationFeature nl = FEATURE_SERVICE.getStorage().getByClass(NoLevitationFeature.class);
        if (nl != null && nl.isEnabled()) {
            if (effect == MobEffects.LEVITATION)
                return null;
//            if (nl.noSlowFall.get() && effect.value == StatusEffects.SLOW_FALLING)
//                return null;
        }
        return original;
    }
}
