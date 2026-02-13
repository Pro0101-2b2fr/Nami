package namidevelopment.kiriyaga.nami.impl.feature.combat;

import namidevelopment.kiriyaga.api.event.EventPriority;
import namidevelopment.kiriyaga.api.annotation.SubscribeEvent;
import namidevelopment.kiriyaga.api.event.impl.PostTickEvent;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;
import namidevelopment.kiriyaga.api.util.EnchantmentUtils;
import namidevelopment.kiriyaga.api.util.entity.TargetUtils;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.*;

import static namidevelopment.kiriyaga.api.NamiApi.INVENTORY_SERVICE;
import static namidevelopment.kiriyaga.api.NamiApi.*;import static namidevelopment.kiriyaga.api.util.entity.PlayerUtils.isBroken;

@RegisterFeature
public class AutoArmorFeature extends Feature {

    public enum ProtectionPriority {PROT, BLAST }
    public enum BootsPriority { LEATHER, GOLDEN, BEST }
    public enum HelmetPriority { BEST, TURTLE, GOLDEN, PUMPKIN, NONE }

    public final EnumSetting<ProtectionPriority> protectionPriority = addSetting(new EnumSetting<>("Protection", ProtectionPriority.PROT));
    public final IntSetting damageThreshold = addSetting(new IntSetting("Durability", 3, 1, 15));
    public final EnumSetting<HelmetPriority> helmetSetting = addSetting(new EnumSetting<>("Helmet", HelmetPriority.BEST));
    public final BoolSetting helmetSafety = addSetting(new BoolSetting("Safety", false));
    public final EnumSetting<BootsPriority> bootsPriority = addSetting(new EnumSetting<>("Boots", BootsPriority.BEST));
    public final BoolSetting elytraPriority = addSetting(new BoolSetting("ElytraPriority", false));
    public final BoolSetting mendingRepair = addSetting(new BoolSetting("MendingRepair", false));

    public static final Set<Item> ARMOR_ITEMS_HEAD = Set.of(Items.LEATHER_HELMET, Items.GOLDEN_HELMET, Items.CHAINMAIL_HELMET, Items.IRON_HELMET, Items.DIAMOND_HELMET, Items.NETHERITE_HELMET, Items.TURTLE_HELMET, Items.CARVED_PUMPKIN);
    public static final Set<Item> ARMOR_ITEMS_CHEST = Set.of(Items.LEATHER_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE, Items.ELYTRA);
    public static final Set<Item> ARMOR_ITEMS_LEGS = Set.of(Items.LEATHER_LEGGINGS, Items.GOLDEN_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS, Items.DIAMOND_LEGGINGS, Items.NETHERITE_LEGGINGS);
    public static final Set<Item> ARMOR_ITEMS_FEET = Set.of(Items.LEATHER_BOOTS, Items.GOLDEN_BOOTS, Items.CHAINMAIL_BOOTS, Items.IRON_BOOTS, Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS);

    public AutoArmorFeature() {
        super("AutoArmor", "Automatically equips best armor.", FeatureCategory.of("Combat"), "autoarmor");
        helmetSafety.setShowCondition(() -> helmetSetting.get() == HelmetPriority.NONE);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onTick(PostTickEvent event) {
        if (MC.level == null || MC.player == null) return;
        Entity target = TargetUtils.getTarget();

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!isArmorSlot(slot)) continue;

            ItemStack current = MC.player.getItemBySlot(slot);

            if (mendingRepair.get()) {
                if (shouldEquipMendingRepair(slot, current)) {
                    ItemStack mendingPiece = findDamagedMendingArmor(slot);
                    if (mendingPiece != null) {
                        int invSlot = findInventorySlot(mendingPiece);
                        if (invSlot != -1) {
                            swap(slot, invSlot);
                            return;
                        }
                    }
                    continue;
                }
                return;
            }

            ItemStack best = findBestForSlot(slot, current, target);

            if (best != null) {
                if (best.isEmpty()) {
                    if (!current.isEmpty()) {
                        int invSlot = convertSlot(findEmptySlot());
                        if (invSlot != -1) {
                            swap(slot, invSlot);
                            return;
                        }
                    }
                } else if (!ItemStack.matches(best, current)) {
                    if (best == MC.player.containerMenu.getCarried()) {
                        swapCursor(slot);
                        return;
                    }

                    int invSlot = findInventorySlot(best);
                    if (invSlot != -1) {
                        swap(slot, invSlot);
                        return;
                    }
                }
            }
        }
    }

    private ItemStack findBestForSlot(EquipmentSlot slot, ItemStack current, Entity target) {
        if (!current.isEmpty() && hasCurse(current)) {
            return null;
        }

        switch (slot) {
            case HEAD:
                if (helmetSetting.get() == HelmetPriority.NONE) {
                    return helmetSafety.get() && target != null ? chooseBest(findCandidatesForSlot(slot, true), current) : ItemStack.EMPTY;
                }
                return chooseBest(findCandidatesForSlot(slot, false), current);
            case FEET:
                return chooseBest(findCandidatesForSlot(slot, false), current);
            case CHEST:
            case LEGS:
                return findBestArmor(slot, current);
            default:
                return null;
        }
    }

    private List<ItemStack> findCandidatesForSlot(EquipmentSlot slot, boolean forceBest) {
        List<ItemStack> candidates = new ArrayList<>();

        ItemStack carried = MC.player.containerMenu.getCarried();
        if (!carried.isEmpty() && isArmorForSlot(carried, slot) && !hasCurse(carried)) {
            candidates.add(carried);
        }

        for (int i = 0; i < 36; i++) {
            ItemStack stack = MC.player.getInventory().getItem(i);
            if (stack.isEmpty())
                continue;
            Item item = stack.getItem();

            if (hasCurse(stack))
                continue;

            if (!isArmorForSlot(stack, slot))
                continue;

            if (slot == EquipmentSlot.HEAD) {
                switch (helmetSetting.get()) {
                    case TURTLE:
                        if (item == Items.TURTLE_HELMET) return Collections.singletonList(stack);
                        break;
                    case GOLDEN:
                        if (item == Items.GOLDEN_HELMET) return Collections.singletonList(stack);
                        break;
                    case PUMPKIN:
                        if (item == Items.CARVED_PUMPKIN) return Collections.singletonList(stack);
                        break;
                    case BEST:
                        candidates.add(stack);
                        break;
                    case NONE:
                        if (forceBest) candidates.add(stack);
                        break;
                }
            } else if (slot == EquipmentSlot.FEET) {
                switch (bootsPriority.get()) {
                    case LEATHER:
                        if (item == Items.LEATHER_BOOTS) return Collections.singletonList(stack);
                        break;
                    case GOLDEN:
                        if (item == Items.GOLDEN_BOOTS) return Collections.singletonList(stack);
                        break;
                    case BEST:
                        candidates.add(stack);
                        break;
                }
            } else {
                if (slot == EquipmentSlot.CHEST && item == Items.ELYTRA && elytraPriority.get()) {
                    candidates.add(stack);
                } else {
                    candidates.add(stack);
                }
            }
        }

        return candidates;
    }

    private ItemStack chooseBest(List<ItemStack> candidates, ItemStack current) {
        if (candidates.isEmpty()) return null;
        ItemStack best = current;
        for (ItemStack candidate : candidates) {
            if (hasCurse(candidate))
                continue;

            if (isBetterArmor(candidate, best)) best = candidate;
        }
        return best != current ? best : null;
    }

    private boolean isBetterArmor(ItemStack a, ItemStack b) {
        if (a.isEmpty()) return false;
        if (b.isEmpty()) return true;

        int aMat = getMaterialScore(a.getItem());
        int bMat = getMaterialScore(b.getItem());
        if (aMat != bMat) return aMat > bMat;

        int aProt = EnchantmentUtils.getEnchantmentLevel(a, Enchantments.PROTECTION);
        int bProt = EnchantmentUtils.getEnchantmentLevel(b, Enchantments.PROTECTION);
        int aBlast = EnchantmentUtils.getEnchantmentLevel(a, Enchantments.BLAST_PROTECTION);
        int bBlast = EnchantmentUtils.getEnchantmentLevel(b, Enchantments.BLAST_PROTECTION);

        if (protectionPriority.get() == ProtectionPriority.BLAST) {
            if (aBlast != bBlast) return aBlast > bBlast;
        } else {
            if (aProt != bProt) return aProt > bProt;
        }

        return (aProt + aBlast) > (bProt + bBlast);
    }

    private ItemStack findBestArmor(EquipmentSlot slot, ItemStack current) {
        List<ItemStack> candidates = findCandidatesForSlot(slot, false);
        if (slot == EquipmentSlot.CHEST && elytraPriority.get()) {
            boolean wearingElytra = current.getItem() == Items.ELYTRA && !isBroken(current, damageThreshold.get());
            if (wearingElytra) return null;
            for (ItemStack stack : candidates) {
                if (stack.getItem() == Items.ELYTRA && !isBroken(stack, damageThreshold.get()) && !hasCurse(stack)) return stack;
            }
        }
        return chooseBest(candidates, current);
    }

    private boolean isArmorSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
    }

    private void swap(EquipmentSlot armorSlot, int slot) {
        if (!MC.player.containerMenu.getCarried().isEmpty()) {
            INVENTORY_SERVICE.getClickHandler().pickupSlot(convertSlot(findEmptySlot()));
            return;
        }

        ItemStack equipped = MC.player.getItemBySlot(armorSlot);
        int realSlot = convertSlot(slot);

        int armorSlotIndex = switch (armorSlot) {
            case HEAD -> 5;
            case CHEST -> 6;
            case LEGS -> 7;
            case FEET -> 8;
            default -> throw new IllegalArgumentException();
        };

        INVENTORY_SERVICE.getClickHandler().pickupSlot(realSlot);
        boolean hasEquipped = !equipped.isEmpty();
        INVENTORY_SERVICE.getClickHandler().pickupSlot(armorSlotIndex);
        if (hasEquipped) INVENTORY_SERVICE.getClickHandler().pickupSlot(realSlot);
    }

    private void swapCursor(EquipmentSlot armorSlot) {
        int armorSlotIndex = switch (armorSlot) {
            case HEAD -> 5;
            case CHEST -> 6;
            case LEGS -> 7;
            case FEET -> 8;
            default -> throw new IllegalArgumentException();
        };

        INVENTORY_SERVICE.getClickHandler().pickupSlot(armorSlotIndex);

        if (!MC.player.containerMenu.getCarried().isEmpty()) {
            int empty = findEmptySlot();
            if (empty != -1) {
                INVENTORY_SERVICE.getClickHandler().pickupSlot(convertSlot(empty));
            }
        }
    }

    private boolean shouldEquipMendingRepair(EquipmentSlot slot, ItemStack current) {
        if (current.isEmpty()) return true;
        if (!hasMending(current)) return true;
        if (isFullyRepaired(current)) return true;
        return false;
    }

    private ItemStack findDamagedMendingArmor(EquipmentSlot slot) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = MC.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (!isArmorForSlot(stack, slot)) continue;
            if (!hasMending(stack)) continue;
            if (isFullyRepaired(stack)) continue;
            return stack;
        }
        return null;
    }

    private boolean hasMending(ItemStack stack) {
        return EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.MENDING) > 0;
    }

    private boolean hasCurse(ItemStack stack) {
        return EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.BINDING_CURSE) > 0;
    }

    private boolean isFullyRepaired(ItemStack stack) {
        if (!stack.isDamageableItem()) return true;
        return stack.getDamageValue() == 0;
    }

    private int findEmptySlot() {
        for (int i = 0; i < 36; i++) {
            if (MC.player.getInventory().getItem(i).isEmpty()) return i;
        }
        return -1;
    }

    private boolean isArmorForSlot(ItemStack stack, EquipmentSlot slot) {
        Item item = stack.getItem();
        return switch (slot) {
            case HEAD -> ARMOR_ITEMS_HEAD.contains(item);
            case CHEST -> ARMOR_ITEMS_CHEST.contains(item);
            case LEGS -> ARMOR_ITEMS_LEGS.contains(item);
            case FEET -> ARMOR_ITEMS_FEET.contains(item);
            default -> false;
        };
    }

    private int findInventorySlot(ItemStack target) {
        for (int i = 0; i < 36; i++) {
            if (ItemStack.matches(MC.player.getInventory().getItem(i), target)) return i;
        }
        return -1;
    }

    private int convertSlot(int slot) {
        return slot < 9 ? slot + 36 : slot;
    }

    private int getMaterialScore(Item item) {
        // yes this is shitcode, no, i dont see any other solution, no more armormaterial etc, only components
        if (item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE || item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS) return 6;
        if (item == Items.DIAMOND_HELMET || item == Items.DIAMOND_CHESTPLATE || item == Items.DIAMOND_LEGGINGS || item == Items.DIAMOND_BOOTS) return 5;
        if (item == Items.IRON_HELMET || item == Items.IRON_CHESTPLATE || item == Items.IRON_LEGGINGS || item == Items.IRON_BOOTS) return 4;
        if (item == Items.CHAINMAIL_HELMET || item == Items.CHAINMAIL_CHESTPLATE || item == Items.CHAINMAIL_LEGGINGS || item == Items.CHAINMAIL_BOOTS) return 3;
        if (item == Items.GOLDEN_HELMET || item == Items.GOLDEN_CHESTPLATE || item == Items.GOLDEN_LEGGINGS || item == Items.GOLDEN_BOOTS) return 2;
        if (item == Items.LEATHER_HELMET || item == Items.LEATHER_CHESTPLATE || item == Items.LEATHER_LEGGINGS || item == Items.LEATHER_BOOTS) return 1;
        return 0;
    }
}
