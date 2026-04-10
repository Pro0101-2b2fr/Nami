package namidevelopment.kiriyaga.nami.impl.gui.entry;

import namidevelopment.kiriyaga.nami.impl.gui.base.BaseEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;

public class WhitelistEntry extends BaseEntry {

    private final String id;
    private final ItemStack icon;

    public WhitelistEntry(String id) {
        this.id = id;
        this.icon = resolveIcon(id);
        refreshEntry();
    }

    private static final Map<String, String> ENTITY_ITEM_ALIASES = Map.of(
            "eye_of_ender", "ender_eye",
            "wither_skull", "wither_skeleton_skull",
            "dragon_fireball", "fire_charge",
            "small_fireball", "fire_charge",
            "fireball", "fire_charge");

    private ItemStack resolveIcon(String idStr) {
        Identifier loc = Identifier.tryParse(idStr);
        if (loc == null)
            return ItemStack.EMPTY;

        // Try as item
        var itemOpt = BuiltInRegistries.ITEM.getOptional(loc);
        if (itemOpt.isPresent()) {
            Item item = itemOpt.get();
            if (item != Items.AIR)
                return new ItemStack(item);
        }

        // Try as block
        var blockOpt = BuiltInRegistries.BLOCK.getOptional(loc);
        if (blockOpt.isPresent()) {
            Item blockItem = blockOpt.get().asItem();
            if (blockItem != Items.AIR)
                return new ItemStack(blockItem);
        }

        // Try as spawn egg if it's an entity
        Identifier eggId = Identifier.tryParse(loc.getNamespace() + ":" + loc.getPath() + "_spawn_egg");
        if (eggId != null) {
            var eggOpt = BuiltInRegistries.ITEM.getOptional(eggId);
            if (eggOpt.isPresent()) {
                Item egg = eggOpt.get();
                if (egg != Items.AIR)
                    return new ItemStack(egg);
            }
        }

        // Try aliases (mostly for entities)
        String alias = ENTITY_ITEM_ALIASES.get(loc.getPath());
        if (alias != null) {
            Identifier aliasId = Identifier.tryParse(loc.getNamespace() + ":" + alias);
            if (aliasId != null) {
                var aliasOpt = BuiltInRegistries.ITEM.getOptional(aliasId);
                if (aliasOpt.isPresent()) {
                    Item aliasItem = aliasOpt.get();
                    if (aliasItem != Items.AIR)
                        return new ItemStack(aliasItem);
                }
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public Component getDisplayText() {
        return displayText;
    }

    @Override
    public void refreshEntry() {
        this.displayText = Component.literal(id);
    }

    public String getId() {
        return id;
    }

    public ItemStack getIcon() {
        return icon;
    }
}
