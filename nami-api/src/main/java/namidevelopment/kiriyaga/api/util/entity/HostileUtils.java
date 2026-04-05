package namidevelopment.kiriyaga.api.util.entity;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.monster.illager.Illusioner;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.skeleton.Bogged;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.skeleton.Stray;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;

import static namidevelopment.kiriyaga.api.NamiApi.*;

public class HostileUtils {
    public static boolean isProjectile(Entity e) {
        return e instanceof ShulkerBullet
                || e instanceof LargeFireball
                || e instanceof WitherSkull
                || e instanceof Arrow
                || e instanceof ThrownTrident
                || e instanceof ThrowableProjectile;
    }

    public static boolean isHostile(Entity e) {
        if (e instanceof Creeper
                || e instanceof Skeleton
                || e instanceof Stray
                || e instanceof WitherSkeleton
                || e instanceof Zombie && !(e instanceof ZombifiedPiglin) // yeah zombified piglin is inherited from
                                                                          // zombie
                || e instanceof Husk
                || e instanceof Drowned
                || e instanceof Vindicator
                || e instanceof Bogged
                || e instanceof Evoker
                || e instanceof Pillager
                || e instanceof Ravager
                || e instanceof Vex
                || e instanceof Breeze
                || e instanceof Blaze
                || e instanceof WitherBoss
                || e instanceof EnderDragon
                || e instanceof Shulker
                || e instanceof Guardian
                || e instanceof ElderGuardian
                || e instanceof Ghast
                || e instanceof Hoglin
                || e instanceof ZombieVillager
                || e instanceof MagmaCube
                || e instanceof Silverfish
                || e instanceof Slime
                || e instanceof Phantom
                || e instanceof Illusioner
                || e instanceof PiglinBrute
                || e instanceof Witch) {
            return true;
        }

        return isNeutralEntityType(e) && isAggressiveNow(e);
    }

    public static boolean isNeutral(Entity e) {
        return isNeutralEntityType(e) && !isAggressiveNow(e);
    }

    public static boolean isPassive(Entity e) {
        return e instanceof AgeableMob ||
                (e instanceof IronGolem golem && golem.isPlayerCreated());
    }

    private static boolean isNeutralEntityType(Entity e) {
        return e instanceof EnderMan ||
                e instanceof Piglin ||
                e instanceof ZombifiedPiglin ||
                e instanceof Spider ||
                e instanceof CaveSpider ||
                e instanceof PolarBear ||
                (e instanceof Wolf w && !w.isTame()) ||
                e instanceof Bee ||
                e instanceof Goat ||
                (e instanceof IronGolem g && !g.isPlayerCreated());
    }

    public static boolean isAggressiveNow(Entity e) {
        LocalPlayer player = MC.player;
        if (player == null || MC.level == null)
            return false;

        long timeOfDay = MC.level.getDayTime() % 24000;
        boolean isNight = timeOfDay >= 13000 && timeOfDay <= 23000;

        if (e instanceof EnderMan enderman)
            return enderman.isCreepy();
        if (e instanceof ZombifiedPiglin piglin)
            return piglin.isAggressive();
        if (e instanceof Piglin piglin)
            return !PlayerUtils.isPlayerWearingGold(player) || piglin.isAggressive();
        if (e instanceof Spider spider)
            return spider.isAggressive() || isNight;
        if (e instanceof CaveSpider)
            return true;
        if (e instanceof PolarBear bear)
            return bear.isAggressive();
        if (e instanceof Wolf wolf)
            return wolf.isAggressive();
        if (e instanceof Bee bee)
            return bee.isAngry();

        return false;
    }
}
