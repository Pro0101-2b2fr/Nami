package namidevelopment.kiriyaga.nami.impl.feature.combat.autocrystal;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.event.impl.AddEntityEvent;
import namidevelopment.kiriyaga.api.event.impl.PreTickEvent;
import namidevelopment.kiriyaga.api.event.impl.RemoveEntityEvent;
import namidevelopment.kiriyaga.api.event.impl.Render3DEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.util.EnchantmentUtils;
import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.core.rotation.model.RotationRequest;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import namidevelopment.kiriyaga.api.util.InteractionUtils;
import namidevelopment.kiriyaga.api.util.entity.DamageUtils;
import namidevelopment.kiriyaga.api.util.entity.EntityUtils;
import namidevelopment.kiriyaga.api.util.render.RenderUtil;
import namidevelopment.kiriyaga.nami.impl.feature.world.SpeedMineFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.EntityHitResult;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static namidevelopment.kiriyaga.api.NamiApi.*;
import static namidevelopment.kiriyaga.api.util.RotationUtils.*;

@RegisterFeature
public class AutoCrystalFeature extends Feature {
    public enum Page {PLACE, BREAK, DAMAGES, RENDER}
    public enum Sequential {NONE, FULL }

    public final EnumSetting<Page> page = addSetting(new EnumSetting<>("Page", Page.PLACE));

    //place
    public final BoolSetting doPlace = addSetting(new BoolSetting("Place", true));
    public final DoubleSetting placeRange = addSetting(new DoubleSetting("PlaceRange","Range", 6.0, 1.0, 6.0));
    public final IntSetting placeDelay = addSetting(new IntSetting("PlaceDelay","Delay", 0, 0, 20));
    public final BoolSetting placeRotate = addSetting(new BoolSetting("PlaceRotate","Rotate", true));
    public final BoolSetting placeSwing = addSetting(new BoolSetting("PlaceSwing","Swing", true));
    public final BoolSetting placeIgnoreItems = addSetting(new BoolSetting("PlaceIgnoreItems","IgnoreItems", true));
    public final BoolSetting placeIgnoreCrystals = addSetting(new BoolSetting("PlaceIgnoreCrystals","IgnoreCrystals", true));
    public final BoolSetting placeStrictDirection = addSetting(new BoolSetting("PlaceStrictDirection","StrictDirection", true));
    public final BoolSetting placeSwapBack = addSetting(new BoolSetting("PlaceSwapBack","SwapBack", true));
    public final BoolSetting placeMultitask = addSetting(new BoolSetting("PlaceMultitask","Multitask", false));
    public final BoolSetting placeIgnoreTerrain = addSetting(new BoolSetting("PlaceIgnoreTerrain","IgnoreTerrain", true));
    public final BoolSetting placeAntiFeetTrap = addSetting(new BoolSetting("PlaceAntiFeetTrap", "AntiFeetTrap", true));
    public final DoubleSetting placeAntiFeetTrapFactor = addSetting(new DoubleSetting("PlaceAntiFeetTrapFactor","Factor", 0.80, 0.5, 1.00));

    //break
    public final BoolSetting doBreak = addSetting(new BoolSetting("Break", true));
    public final DoubleSetting breakRange = addSetting(new DoubleSetting("BreakRange","Range", 3.0, 1.0, 7.0));
    public final IntSetting breakInhibit = addSetting(new IntSetting("Inhibit", 4, 1, 20));
    public final IntSetting breakDelay = addSetting(new IntSetting("BreakDelay","Delay", 0, 0, 20));
    public final BoolSetting breakRotate = addSetting(new BoolSetting("BreakRotate","Rotate", true));
    public final BoolSetting breakSwing = addSetting(new BoolSetting("BreakSwing","Swing", true));
    public final BoolSetting breakMultitask = addSetting(new BoolSetting("BreakMultitask","Multitask", false));
    public final IntSetting breakAge = addSetting(new IntSetting("Age", 0, 0, 20));
    public final EnumSetting<Sequential> breakSequential = addSetting(new EnumSetting<>("BreakSequential","Sequential", Sequential.NONE));

    //damages
    public final BoolSetting assumeBestArmor = addSetting(new BoolSetting("AssumeBestArmor", true));
    public final BoolSetting noSelfPop = addSetting(new BoolSetting("NoSelfPop", true));
    public final DoubleSetting minDamage = addSetting(new DoubleSetting("MinDamage", 4.0, 0.0, 36.0));
    public final DoubleSetting maxSelfDamage = addSetting(new DoubleSetting("MaxSelfDamage", 12.0, 0.0, 36.0));
    public final BoolSetting facePlace = addSetting(new BoolSetting("FacePlace", true));
    public final DoubleSetting facePlaceHealth = addSetting(new DoubleSetting("FacePlaceHealth", 10.0, 0.0, 36.0));

    //render
    public final BoolSetting render = addSetting(new BoolSetting("Render", true));
    public final BoolSetting debug = addSetting(new BoolSetting("Debug", false));

    private int breakTimer, placeTimer = 0; // i love it
    private PlaceTarget lastPlaceTarget = null;
    public float lastTotalDamage;
    float lastCalcTimeMs = 0;
    private final Int2IntOpenHashMap crystalHits = new Int2IntOpenHashMap();
    private final Long2IntOpenHashMap crystalPlaces = new Long2IntOpenHashMap();
    private final ExecutorService calcExecutor = Executors.newSingleThreadExecutor();
    private volatile Future<?> runningTask;
    private final AtomicReference<PlaceTarget> asyncBest = new AtomicReference<>();
    private volatile PlaceTarget bestPlace;


    public AutoCrystalFeature() {
        super("AutoCrystal", "Automatically places and break crystals to kill people, if you are good enough!.", FeatureCategory.of("Combat"), "autocrystal", "ac", "crystalaura");
        debug.setShow(false);

        doBreak.setShowCondition(() -> page.get() == Page.BREAK);
        breakRange.setShowCondition(() -> doBreak.get() && page.get() == Page.BREAK);
        breakDelay.setShowCondition(() -> doBreak.get() && page.get() == Page.BREAK);
        breakRotate.setShowCondition(() -> doBreak.get() && page.get() == Page.BREAK);
        breakSwing.setShowCondition(() -> doBreak.get() && page.get() == Page.BREAK);
        breakMultitask.setShowCondition(() -> doBreak.get() && page.get() == Page.BREAK);
        breakAge.setShowCondition(() -> doBreak.get() && page.get() == Page.BREAK);
        breakSequential.setShowCondition(() -> doBreak.get() && page.get() == Page.BREAK);
        breakInhibit.setShowCondition(() -> doBreak.get() && page.get() == Page.BREAK);

        doPlace.setShowCondition(() -> page.get() == Page.PLACE);
        placeRange.setShowCondition(() -> doPlace.get() && page.get() == Page.PLACE);
        placeDelay.setShowCondition(() -> doPlace.get() && page.get() == Page.PLACE);
        placeRotate.setShowCondition(() -> doPlace.get() && page.get() == Page.PLACE);
        placeSwing.setShowCondition(() -> doPlace.get() && page.get() == Page.PLACE);
        placeIgnoreItems.setShowCondition(() -> doPlace.get() && page.get() == Page.PLACE);
        placeMultitask.setShowCondition(() -> doPlace.get() && page.get() == Page.PLACE);
        placeSwapBack.setShowCondition(() -> doPlace.get() && page.get() == Page.PLACE);
        placeIgnoreCrystals.setShowCondition(() -> doPlace.get() && page.get() == Page.PLACE);
        placeStrictDirection.setShowCondition(() -> doPlace.get() && page.get() == Page.PLACE);
        placeIgnoreTerrain.setShowCondition(() -> doPlace.get() && page.get() == Page.PLACE);
        placeAntiFeetTrap.setShowCondition(() -> doPlace.get() && page.get() == Page.PLACE);
        placeAntiFeetTrapFactor.setShowCondition(() -> doPlace.get() && page.get() == Page.PLACE && placeAntiFeetTrap.get());

        noSelfPop.setShowCondition(() ->  page.get() == Page.DAMAGES);
        minDamage.setShowCondition(() -> page.get() == Page.DAMAGES);
        maxSelfDamage.setShowCondition(() -> page.get() == Page.DAMAGES);
        assumeBestArmor.setShowCondition(() -> page.get() == Page.DAMAGES);
        facePlace.setShowCondition(() -> page.get() == Page.DAMAGES);
        facePlaceHealth.setShowCondition(() -> page.get() == Page.DAMAGES && facePlace.get());

        render.setShowCondition(() -> page.get() == Page.RENDER);
    }

    @Override
    public void onEnable() {
        breakTimer = 0;
        placeTimer = 0;
        lastTotalDamage = 0;
        lastCalcTimeMs = 0;
        lastPlaceTarget = null;
        crystalPlaces.clear();
        crystalHits.clear();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPreTickEvent(PreTickEvent event) {
        long start = System.nanoTime();
        if (MC.player == null || MC.player.isDeadOrDying()) return;

        update();

        long tickId = MC.level.getGameTime();

        if (runningTask == null || runningTask.isDone()) {
            AutoCrystalSnapshot.AsyncDebugInfo dbg = new AutoCrystalSnapshot.AsyncDebugInfo(tickId);

            AutoCrystalSnapshot snap = doSnapshot(tickId, dbg);
            runningTask = calcExecutor.submit(() -> {
                PlaceTarget best = findNextPlaceTargetForSnapshot(snap, dbg);
                asyncBest.set(best);

                if (debug.get()) {
                    float ms = (System.nanoTime() - dbg.startNs) / 1_000_000f;

                    MC.execute(() -> {
                        CHAT_SERVICE.sendPersistent(
                                "AutoCrystalFeature#asyncCalc",
                                dbg.buildMessage(ms)
                        );
                    });
                }
            });
        }

        if (this.asyncBest.get() != null) {
            BlockPos pos = this.asyncBest.get().pos;
            BlockPos base = pos.below();

            Vec3 crystalPos = new Vec3(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);

            float realDamage = calculateDamage(crystalPos, true);
            if (realDamage >= minDamage.get()) {
                bestPlace = new PlaceTarget(pos, realDamage);
                lastTotalDamage = realDamage;
            } else {
                bestPlace = null;
            }
        } else {
            bestPlace = null;
        }

        if (doBreak.get()) {
            if (breakTimer > 0) breakTimer--;
            else doBreak();
        }

        if (doPlace.get()) {
            if (placeTimer > 0) placeTimer--;
            else {
                if (bestPlace != null) {
                    doPlace(bestPlace);
                }
            }
        }

        lastCalcTimeMs = (System.nanoTime() - start) / 1_000_000f;

        this.clearDisplayInfo();
        this.addDisplayInfo(String.format(Locale.US, "%.2f", lastTotalDamage));
        this.addDisplayInfo(String.format(Locale.US, "%.4f", lastCalcTimeMs));
    }

    @SubscribeEvent
    private void onRemoveEntityEvent(RemoveEntityEvent event) {
        ClientboundRemoveEntitiesPacket packet = event.getPacket();
        for (int id : packet.getEntityIds()) {
            if (crystalHits.containsKey(id)) {
                crystalHits.remove(id);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    private void onAddEntityEvent(AddEntityEvent event) {
        if (breakSequential.get() != Sequential.FULL) return;

        if (event.getPacket() instanceof ClientboundAddEntityPacket packet) {
            if (packet.getType() != EntityType.END_CRYSTAL) {
                return;
            }

            Vec3 pos = new Vec3(packet.getX(), packet.getY(), packet.getZ());
            EndCrystal fake = new EndCrystal(EntityType.END_CRYSTAL, MC.level);
            fake.setPos(pos);
            fake.setId(packet.getId());

            doBreakOnNetty(fake);
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onRender3DEvent(Render3DEvent event) {
        if (MC.level == null || MC.player == null || lastPlaceTarget == null || !render.get()) return;

        BlockPos pos = lastPlaceTarget.pos.below();
        AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
        Color color = FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class).getStyledGlobalColor();

        RenderUtil.drawBoxLines(box, color, true, true, 1.5f);
    }

    private void doBreakOnNetty(EndCrystal crystal) { // we are not on netty actually
        if (!breakMultitask.get() && MC.player.isUsingItem())  {
            return;
        }

        if (breakRotate.get()) {
            Vec3 hit = getClosestPointToEye(MC.player.getEyePosition(), crystal.getBoundingBox());

            float yaw = (float) getYawToVec(MC.player, hit);
            float pitch = (float) getPitchToVec(MC.player, hit);

            ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(AutoCrystalFeature.class.getName(), 9, yaw, pitch));
        }

        if (!canBreak(crystal)) {
            return;
        }

        MC.player.connection.send(ServerboundInteractPacket.createAttackPacket(crystal, MC.player.isShiftKeyDown()));

        if (breakSwing.get())
            MC.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));

        //breakTimer = breakDelay.get();
    }

    private void doBreak() {
        BreakTarget target = bestCrystal();
        if (target == null) return;

        if (!breakMultitask.get() && MC.player.isUsingItem()) return;

        if (breakRotate.get()) {
            Vec3 pos = getClosestPointToEye(MC.player.getEyePosition(), target.crystal.getBoundingBox());
            float yaw = (float) getYawToVec(MC.player, pos);
            float pitch = (float) getPitchToVec(MC.player, pos);

            ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(AutoCrystalFeature.class.getName(), 9, yaw, pitch));
        }

        if (!canBreak(target.crystal)) return;

        int id = target.crystal.getId();
        int hits = crystalHits.get(id);

        if (hits >= breakInhibit.get() && target.crystal.tickCount < 20)
            return;

        MC.gameMode.attack(MC.player, target.crystal);

        if (breakSwing.get())
            MC.player.swing(InteractionHand.MAIN_HAND);

        crystalHits.put(id, hits + 1);

        breakTimer = breakDelay.get();
    }

    private BreakTarget bestCrystal() {
        BreakTarget best = null;

        for (Entity e : EntityUtils.getEntities(EntityUtils.EntityTypeCategory.END_CRYSTALS, 10)) {
            if (!(e instanceof EndCrystal crystal)) continue;

            if (e.tickCount < breakAge.get()) continue;

            long posKey = crystal.blockPosition().asLong();

            if (crystal.tickCount < 20 && !crystalPlaces.containsKey(posKey))
                continue;

            //   if (MC.player.distanceToSqr(crystal) > 10 * 10) continue;

            Vec3 pos = getClosestPointToEye(MC.player.getEyePosition(), crystal.getBoundingBox());
            float yaw = (float) getYawToVec(MC.player, pos);
            float pitch = (float) getPitchToVec(MC.player, pos);
            EntityHitResult perfect = raycastTarget(MC.player, crystal, breakRange.get(), yaw, pitch);
            boolean insideBox = crystal.getBoundingBox().contains(MC.player.getEyePosition());

            if (!insideBox && perfect == null) continue;

            float totalDamage = calculateDamage(crystal.position(), false);
            if (totalDamage <= -0.9f)
                continue;

            if (best == null || totalDamage > best.totalDamage)
                best = new BreakTarget(crystal, totalDamage);
        }

        if (best != null && best.totalDamage < minDamage.get()/2)
            return null;

        return best;
    }

    private boolean canBreak(EndCrystal crystal) {
        if (!breakRotate.get())
            return true;

      //  ROTATION_SERVICE.getRequestHandler().submit(new RotationRequest(AutoCrystalFeature.class.getName(), 5, idealYaw, idealPitch));
        boolean insideBox = crystal.getBoundingBox().contains(MC.player.getEyePosition(1.0f));
        EntityHitResult serverCheck = raycastTarget(MC.player, crystal, breakRange.get(), ROTATION_SERVICE.getStateHandler().getServerYaw(), ROTATION_SERVICE.getStateHandler().getServerPitch());
        return serverCheck != null || insideBox;
    }

    private void doPlace(PlaceTarget target) {
        if (target == null) return;
        if (target.totalDamage < minDamage.get()) return;

        InteractionUtils.interactBlockAt(target.pos.below(), Items.END_CRYSTAL, null, placeSwapBack.get(), placeMultitask.get(), placeRange.get(), placeRotate.get(), placeStrictDirection.get(), false, placeSwing.get(), AutoCrystalFeature.class.getName() + "_PLACE");

        lastPlaceTarget = target;

        crystalPlaces.put(target.pos.asLong(), 0);
        placeTimer = placeDelay.get();
    }

    private AutoCrystalSnapshot doSnapshot(long tickId, AutoCrystalSnapshot.AsyncDebugInfo dbg) {
        var player = MC.player;
        var level = MC.level;

        Vec3 eyePos = player.getEyePosition();
        BlockPos playerPos = player.blockPosition();

        double pr = placeRange.get();
        double br = breakRange.get();
        double minDmg = minDamage.get();

        List<LivingEntity> entities = EntityUtils.getEntities(EntityUtils.EntityTypeCategory.PLAYERS, 12).stream().filter(e -> e instanceof LivingEntity).map(e -> (LivingEntity) e).toList();
        dbg.targetsTotal = entities.size();

        ArrayList<AutoCrystalSnapshot.TargetData> targets = new ArrayList<>();

        for (LivingEntity e : entities) {
            if (e == player)
                continue;
            if (e.isDeadOrDying())
                continue;

            dbg.targetsValid++;

            int resistanceAmp = -1;
            MobEffectInstance eff = e.getEffect(MobEffects.RESISTANCE);
            if (eff != null) resistanceAmp = eff.getAmplifier();

            byte mask = 0;
            if (!e.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) mask |= 1;
            if (!e.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) mask |= 2;
            if (!e.getItemBySlot(EquipmentSlot.LEGS).isEmpty()) mask |= 4;
            if (!e.getItemBySlot(EquipmentSlot.FEET).isEmpty()) mask |= 8;

            int prot = 0;
            int blastProt = 0;

            if (!assumeBestArmor.get()) {
                for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
                    ItemStack stack = e.getItemBySlot(slot);
                    if (stack.isEmpty())
                        continue;

                    prot += EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.PROTECTION);
                    blastProt += EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.BLAST_PROTECTION);
                }
            }

            targets.add(new AutoCrystalSnapshot.TargetData(e.getId(), e.position(), e.getBoundingBox(), (float) Math.floor(e.getAttributeValue(Attributes.ARMOR)), (float) e.getAttributeValue(Attributes.ARMOR_TOUGHNESS), resistanceAmp, mask, prot, blastProt, e.getHealth(), e.getAbsorptionAmount()));
        }

        int r = (int) Math.ceil(pr);
        int rr = r * r;

        ArrayList<BlockPos> candidates = new ArrayList<>();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {

                    if (x * x + y * y + z * z > rr)
                        continue;

                    BlockPos pos = playerPos.offset(x, y, z);
                    BlockPos base = pos.below();

                    BlockState baseState = level.getBlockState(base);
                    if (!baseState.is(Blocks.OBSIDIAN) && !baseState.is(Blocks.BEDROCK)) {
                        dbg.candidatesBadBase++;
                        continue;
                    }

                    if (!level.getBlockState(pos).isAir()) {
                        dbg.candidatesNotAir++;
                        continue;
                    }

                    AABB blockBox = new AABB(pos);
                    if (eyePos.distanceTo(getClampClosestPoint(eyePos, blockBox)) > pr) {
                        dbg.candidatesOutPlaceRange++;
                        continue;
                    }

                    Vec3 crystalPos = new Vec3(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);
                    AABB crystalBox = new AABB(crystalPos.x-1, crystalPos.y, crystalPos.z-1, crystalPos.x+1, crystalPos.y + 2.0, crystalPos.z + 1.0);

                    if (eyePos.distanceTo(getClampClosestPoint(eyePos, crystalBox)) > br) {
                        dbg.candidatesOutBreakRange++;
                        continue;
                    }

                    AABB checkIntersects = new AABB(
                            base.getX(), base.getY() + 1, base.getZ(),
                            base.getX() + 1, base.getY() + 2, base.getZ() + 1
                    );

                    boolean blocked = false;
                    for (Entity e : MC.level.getEntities(null, checkIntersects)) {
                        if (placeIgnoreItems.get() && e instanceof ItemEntity item && item.getAge() <= 5) continue;
                        if (placeIgnoreCrystals.get() && e instanceof EndCrystal crystal && crystal.tickCount < 5) continue;
                        if (e instanceof EndCrystal crystal && crystal.blockPosition().equals(pos)) continue;
                        blocked = true;
                        break;
                    }

                    if (blocked) {
                        dbg.candidatesBlocked++;
                        continue;
                    }
                    dbg.candidatesTotal++;
                    candidates.add(pos);
                }
            }
        }

        return new AutoCrystalSnapshot(tickId, MC.player.getId(), eyePos, playerPos, pr, br, minDmg, assumeBestArmor.get(), facePlace.get(), facePlaceHealth.get(), targets.toArray(new AutoCrystalSnapshot.TargetData[0]), candidates.toArray(new BlockPos[0]));
    }

    private PlaceTarget findNextPlaceTargetForSnapshot(AutoCrystalSnapshot snap, AutoCrystalSnapshot.AsyncDebugInfo dbg) {
        PlaceTarget best = null;

        for (BlockPos pos : snap.candidatePos()) {
            BlockPos base = pos.below();

            Vec3 crystalPos = new Vec3(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);

            float dmg = calculateDamageForSnapshot(crystalPos, snap, dbg);
            if (dmg < snap.minDamage()) continue;

            if (best == null || dmg > best.totalDamage) {
                best = new PlaceTarget(pos, dmg);
            }
        }

        if (best != null)
            dbg.bestFound = 1;
        return best;
    }

    private float calculateDamageForSnapshot(Vec3 explosionPos, AutoCrystalSnapshot snap, AutoCrystalSnapshot.AsyncDebugInfo dbg) {
        float total = 0.0f;

        for (var t : snap.targets()) {
            double dist = t.pos().distanceTo(explosionPos);
            if (dist > 12.0) continue;

            double impact = 1.0 - (dist / 12.0);
            if (impact <= 0.0) continue;

            float baseDamage = (float)((impact * impact + impact) / 2.0 * 7.0 * 12.0 + 1.0);
            float dmg = applyResistanceForSnapshot(baseDamage, t, snap.assumeBestArmor());

            if (t.id() == snap.selfId()) {
                if (dmg > maxSelfDamage.get()) {
                    dbg.dmgRejectedSelf++;
                    return -1f;
                }

                if (noSelfPop.get() && dmg + 1.5f >= t.health() + t.absorption()) {
                    dbg.dmgRejectedNoSelfPop++;
                    return -1f;
                }

                continue;
            }
            if (dmg < snap.minDamage()) {
                if (snap.facePlace() && (t.health() + t.absorption() <= snap.facePlaceHealth())) {
                    if (dmg < 1.5f) {
                        dbg.dmgRejectedMin++;
                        return -1f;
                    }
                } else {
                    dbg.dmgRejectedMin++;
                    return -1f;
                }
            }
            total += dmg;
        }

        return total;
    }

    private float applyResistanceForSnapshot(float damage, AutoCrystalSnapshot.TargetData t, boolean assumeBestArmor) {

        damage *= 1.5f; // it should be hard level, but idk, we leave it at 1.5f, which means it always difficulty: hard for calculations

        if (t.resistanceAmp() >= 0) {
            damage *= 1.0f - 0.2f * (t.resistanceAmp() + 1);
        }

        int totalProtection = 0;

        if (assumeBestArmor) {
            if ((t.armorMask() & 1) != 0) totalProtection += 4;
            if ((t.armorMask() & 2) != 0) totalProtection += 4;
            if ((t.armorMask() & 8) != 0) totalProtection += 4;

            if ((t.armorMask() & 4) != 0) totalProtection += 8;

        } else {
            totalProtection += t.prot();
            totalProtection += 2 * t.blastProt();
        }

        damage = CombatRules.getDamageAfterMagicAbsorb(damage, (float) totalProtection);

        return Math.max(damage, 0.0f);
    }

    private Set<BlockPos> ignoredBlocks(boolean b) {
        Set<BlockPos> ignored = new HashSet<>();

        if (placeIgnoreTerrain.get()) {
            int r = placeRange.get().intValue()+2;
            BlockPos center = MC.player.blockPosition();

            for (int x = -r; x <= r; x++) {
                for (int y = -r; y <= r; y++) {
                    for (int z = -r; z <= r; z++) {
                        BlockPos pos = center.offset(x, y, z);

                        BlockState state = MC.level.getBlockState(pos);
                        if (state.isAir()) continue;

                        if (state.getBlock().getExplosionResistance() < 600) {
                            ignored.add(pos);
                        }
                    }
                }
            }
        }

        if (b && placeAntiFeetTrap.get()) {
            SpeedMineFeature sm = FEATURE_SERVICE.getStorage().getByClass(SpeedMineFeature.class);

            if (sm != null) {
                if (sm.currentTask != null && sm.currentTask.getProgress() >= placeAntiFeetTrapFactor.get()) {
                    ignored.add(sm.currentTask.getBlockPos());
                }

                if (sm.doubleMineTask != null && sm.doubleMineTask.getProgress() >= placeAntiFeetTrapFactor.get()) {
                    ignored.add(sm.doubleMineTask.getBlockPos());
                }
            }
        }

        return ignored;
    }

    private float calculateDamage(Vec3 crystalPos, boolean b) {
        float totalDamage = 0f;
        Set<BlockPos> ignored = ignoredBlocks(b);

        for (Entity e : EntityUtils.getEntities(EntityUtils.EntityTypeCategory.PLAYERS, 12)) {
            if (!(e instanceof LivingEntity living)) continue;

            float dmg = DamageUtils.crystalDamage(living, living.position(), living.getBoundingBox(), crystalPos, DamageUtils.BLOCK_CHECK, assumeBestArmor.get(), ignored);

            if (e == MC.player) {
                if (dmg > maxSelfDamage.get())
                    return -1f;

                if (dmg + 1.5f >= MC.player.getHealth() + MC.player.getAbsorptionAmount())
                    return -1f;
            }

            if (FRIEND_SERVICE.isFriend(e.getName().getString())) continue;

            if (dmg < minDamage.get()) {
                if (facePlace.get() && (living.getHealth() + living.getAbsorptionAmount() <= facePlaceHealth.get())) {
                    if (dmg < 1.5f) // idk if lower would be good too
                        return -1f;
                } else {
                    return -1f;
                }
            }

            totalDamage += dmg;
        }
        return totalDamage;
    }

    private void update() {
        lastPlaceTarget = null;
        lastTotalDamage = 0;
        lastCalcTimeMs = 0;

        LongIterator it = crystalPlaces.keySet().iterator();
        while (it.hasNext()) {
            long key = it.nextLong();
            int age = crystalPlaces.get(key) + 1;

            if (age > 35) {
                it.remove();
            } else {
                crystalPlaces.put(key, age);
            }
        }
    }

    private record PlaceTarget(BlockPos pos, float totalDamage) {}
    private record BreakTarget(EndCrystal crystal, float totalDamage) {}
}
