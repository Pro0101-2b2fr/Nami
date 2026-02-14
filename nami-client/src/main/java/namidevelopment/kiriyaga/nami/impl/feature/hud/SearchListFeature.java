package namidevelopment.kiriyaga.nami.impl.feature.hud;

import namidevelopment.kiriyaga.api.model.feature.HudElementFeature;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.nami.impl.feature.visuals.blocksearch.BlockSearchFeature;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;
@RegisterFeature
public class SearchListFeature extends HudElementFeature {

    public enum SortMode {
        ALPHABETICAL,
        DESCENDING,
        ASCENDING
    }

    private final List<TextElement> elements = new ArrayList<>();

    public final EnumSetting<SortMode> sortMode = addSetting(new EnumSetting<>("Sort", SortMode.DESCENDING));

    public SearchListFeature() {
        super("SearchList", "Shows nearby blocks found by search.", 0, 0, 50, 10);
    }

    @Override
    public List<TextElement> getTextElements() {
        elements.clear();

        var searchFeature = FEATURE_SERVICE.getStorage().getByClass(BlockSearchFeature.class);
        if (searchFeature == null) return elements;

        ConcurrentMap<Long, Set<BlockPos>> chunkBlocks = BlockSearchFeature.chunkBlocks;
        if (chunkBlocks.isEmpty()) return elements;

        Map<String, Integer> blockCounts = new HashMap<>();

        for (Set<BlockPos> blockSet : chunkBlocks.values()) {
            for (BlockPos pos : blockSet) {
                BlockState state = MC.level.getBlockState(pos);
                var blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                if (blockId == null) continue;

                String name = blockId.getPath();
                blockCounts.put(name, blockCounts.getOrDefault(name, 0) + 1);
            }
        }

        List<String> sortedNames = new ArrayList<>(blockCounts.keySet());

        switch (sortMode.get()) {
            case ALPHABETICAL -> sortedNames.sort(String::compareToIgnoreCase);
            case DESCENDING -> sortedNames.sort((a, b) -> Integer.compare(
                    getTextWidth(b, blockCounts.get(b)),
                    getTextWidth(a, blockCounts.get(a))
            ));
            case ASCENDING -> sortedNames.sort((a, b) -> Integer.compare(
                    getTextWidth(a, blockCounts.get(a)),
                    getTextWidth(b, blockCounts.get(b))
            ));
        }

        int yOffset = 0;
        int maxWidth = 0;

        for (String name : sortedNames) {
            int count = blockCounts.get(name);
            Component text = CAT_FORMAT.format("{global}" + name + (count > 1 ? " {secondary}(x" + count + ")" : ""));
            int textWidth = FONT_SERVICE.getWidth(text);
            elements.add(new TextElement(text, 0, yOffset));

            maxWidth = Math.max(maxWidth, textWidth);
            yOffset += FONT_SERVICE.getHeight();
        }

        this.width = maxWidth;
        this.height = yOffset;

        return elements;
    }

    private int getTextWidth(String name, int count) {
        Component text = CAT_FORMAT.format("{global}" + name + (count > 1 ? " {secondary}(x" + count + ")" : ""));
        return FONT_SERVICE.getWidth(text);
    }
}