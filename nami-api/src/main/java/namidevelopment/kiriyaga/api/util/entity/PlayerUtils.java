package namidevelopment.kiriyaga.api.util.entity;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Arrays;

import static namidevelopment.kiriyaga.api.NamiApi.*;

public class PlayerUtils {
    static boolean isPlayerWearingGold(LocalPlayer player) {
        return Arrays.stream(EquipmentSlot.values())
                .filter(EquipmentSlot::isArmor)
                .map(player::getItemBySlot)
                .anyMatch(PlayerUtils::isGoldArmor);
    }

    private static boolean isGoldArmor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item == Items.GOLDEN_HELMET ||
                item == Items.GOLDEN_CHESTPLATE ||
                item == Items.GOLDEN_LEGGINGS ||
                item == Items.GOLDEN_BOOTS;
    }

    public static boolean isItemAWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        Item item = stack.getItem();
        return item instanceof AxeItem
                || stack.is(ItemTags.SWORDS)
                || item instanceof TridentItem
                || item instanceof MaceItem;
    }

    public static boolean isBroken(ItemStack stack, int threshold) {
        if (!stack.isDamageableItem()) return false;
        int max = stack.getMaxDamage();
        int damage = stack.getDamageValue();
        int percentRemaining = (int) (((max - damage) / (float) max) * 100);
        return percentRemaining <= threshold;
    }

    public static boolean isPhased(Entity e) {
        if (e == null || MC.level == null) return false;

        AABB box = e.getBoundingBox();
        int minX = Mth.floor(box.minX);
        int maxX = Mth.ceil(box.maxX);
        int minY = Mth.floor(box.minY);
        int maxY = Mth.ceil(box.maxY);
        int minZ = Mth.floor(box.minZ);
        int maxZ = Mth.ceil(box.maxZ);

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    VoxelShape shape = MC.level.getBlockState(pos).getCollisionShape(MC.level, pos);
                    if (!shape.isEmpty() && shape.bounds().move(pos).intersects(box)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static int getTotemCount(){
        int totemCount = 0;
        for (ItemStack stack : MC.player.getInventory().getNonEquipmentItems()) {
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                totemCount += stack.getCount();
            }
        }
        ItemStack offHandStack = MC.player.getOffhandItem();
        if (offHandStack.getItem() == Items.TOTEM_OF_UNDYING) {
            totemCount += offHandStack.getCount();
        }
        return totemCount;
    }
}
