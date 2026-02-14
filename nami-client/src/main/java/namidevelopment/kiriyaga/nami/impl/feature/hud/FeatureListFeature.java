package namidevelopment.kiriyaga.nami.impl.feature.hud;

import namidevelopment.kiriyaga.api.model.feature.HudElementFeature;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import net.minecraft.network.chat.Component;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import static namidevelopment.kiriyaga.api.NamiApi.*;

@RegisterFeature
public class FeatureListFeature extends HudElementFeature {

    public enum SortMode {
        ALPHABETICAL,
        DESCENDING,
        ASCENDING
    }

    private final List<TextElement> elements = new ArrayList<>();
    private final Map<String, FeatureAnimationState> animationStates = new ConcurrentHashMap<>();
    private int cachedWidth = 0;
    private int cachedHeight = 0;

    public final BoolSetting showDisplayName = addSetting(new BoolSetting("ShowDisplay", true));
    public final EnumSetting<SortMode> sortMode = addSetting(new EnumSetting<>("Sort", SortMode.DESCENDING));

    public FeatureListFeature() {
        super("FeatureList", "Shows enabled and drawn Features.", 0, 0, 50, 10);
    }

    private static class FeatureAnimationState {
        public float progress = 0f;
        public long startTime;
        public int textWidth;
    }

    @Override
    public Rectangle getBoundingBox() {
        return new Rectangle(0, 0, cachedWidth, cachedHeight);
    }

    @Override
    public List<TextElement> getTextElements() {
        long currentTime = System.currentTimeMillis();
        elements.clear();

        List<Feature> activeFeatures = new ArrayList<>(FEATURE_SERVICE.getStorage().getAll().stream().filter(Feature::isEnabled).filter(Feature::isDrawn).toList());

        animationStates.keySet().removeIf(name -> activeFeatures.stream().noneMatch(Feature -> Feature.getName().equals(name)));

        for (Feature Feature : activeFeatures) {
            String name = Feature.getName();
            if (!animationStates.containsKey(name)) {
                FeatureAnimationState state = new FeatureAnimationState();
                state.startTime = currentTime;

                List<Component> displayInfos = showDisplayName.get() ? Feature.getDisplayInfo() : null;

                String rawText;

                if (displayInfos != null && !displayInfos.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(Feature.getName()).append(" {secondary}[");

                    for (int i = 0; i < displayInfos.size(); i++) {
                        Component info = displayInfos.get(i);
                        if (info == null) continue;

                        sb.append("{white}").append(info.getString());

                        if (i < displayInfos.size() - 1) {
                            sb.append("{secondary},");
                        }
                    }
                    sb.append("{secondary}]");
                    rawText = sb.toString();

                } else {
                    rawText = Feature.getName();
                }
                Component formattedText = CAT_FORMAT.format("{global}" + rawText);
                state.textWidth = FONT_SERVICE.getWidth(formattedText);
                animationStates.put(name, state);
            }
        }

        for (FeatureAnimationState state : animationStates.values()) {
            long elapsed = currentTime - state.startTime;
            float normalizedTime = Math.min(elapsed / 150f, 1f);

            if (normalizedTime < 0.5f) {
                state.progress = 2 * normalizedTime * normalizedTime;
            } else {
                float t = normalizedTime * 2 - 2;
                state.progress = -0.5f * (t * t - 2);
            }

            if (elapsed >= 150) {
                state.progress = 1f;
            }
        }

        if (activeFeatures.isEmpty()) {
            return elements;
        }

        List<FeatureTextInfo> FeatureTexts = new ArrayList<>();
        for (Feature Feature : activeFeatures) {
            List<Component> displayInfos = showDisplayName.get() ? Feature.getDisplayInfo() : null;
            String rawText;
            String formattedTextStr;

            if (displayInfos != null && !displayInfos.isEmpty()) {
                StringBuilder rawSb = new StringBuilder();
                rawSb.append(Feature.getName()).append(" [");
                StringBuilder formattedSb = new StringBuilder();
                formattedSb.append("{global}").append(Feature.getName()).append(" {secondary}[");

                for (int i = 0; i < displayInfos.size(); i++) {
                    Component info = displayInfos.get(i);
                    if (info == null) continue;

                    rawSb.append(info.getString());
                    formattedSb.append("{white}").append(info.getString());

                    if (i < displayInfos.size() - 1) {
                        rawSb.append(",");
                        formattedSb.append("{secondary},");
                    }
                }

                rawSb.append("]");
                formattedSb.append("{secondary}]");

                rawText = rawSb.toString();
                formattedTextStr = formattedSb.toString();

            } else {
                rawText = Feature.getName();
                formattedTextStr = "{global}" + Feature.getName();
            }
            Component formattedText = CAT_FORMAT.format(formattedTextStr);
            int width = FONT_SERVICE.getWidth(formattedText);
            FeatureTexts.add(new FeatureTextInfo(Feature, formattedText, rawText, width));
        }

        switch (sortMode.get()) {
            case ALPHABETICAL -> FeatureTexts.sort(Comparator.comparing(info -> info.rawText, String::compareToIgnoreCase));
            case DESCENDING -> FeatureTexts.sort((a, b) -> Integer.compare(b.width, a.width));
            case ASCENDING -> FeatureTexts.sort(Comparator.comparingInt(a -> a.width));
        }

        int yOffset = 0;
        int maxWidth = 0;

        for (FeatureTextInfo info : FeatureTexts) {
            maxWidth = Math.max(maxWidth, info.width);
            yOffset += FONT_SERVICE.getHeight();
        }

        cachedWidth = maxWidth;
        cachedHeight = yOffset;
        this.width = cachedWidth;
        this.height = cachedHeight;

        yOffset = 0;
        for (FeatureTextInfo info : FeatureTexts) {
            String FeatureName = info.Feature.getName();
            FeatureAnimationState state = animationStates.get(FeatureName);

            if (state == null || state.progress <= 0f) {
                yOffset += FONT_SERVICE.getHeight();
                continue;
            }

            Component text = info.formattedText;

            int animatedOffsetX = 0;
            switch (alignment.get()) {
                case LEFT:
                    animatedOffsetX = (int) ((state.progress - 1) * state.textWidth);
                    break;
                case CENTER:
                    animatedOffsetX = (int) ((1 - state.progress) * -state.textWidth);
                    break;
                case RIGHT:
                    animatedOffsetX = (int) ((state.progress - 1) * state.textWidth);
                    break;
            }

            elements.add(new TextElement(text, animatedOffsetX, yOffset));
            yOffset += FONT_SERVICE.getHeight();
        }

        return elements;
    }

    @Override
    public int getRenderXForElement(TextElement element) {
        int baseX = getRenderX();
        int lineWidth = FONT_SERVICE.getWidth(element.text());

        return switch (alignment.get()) {
            case LEFT -> baseX + element.offsetX();
            case CENTER -> baseX + (width - lineWidth) / 2 + element.offsetX();
            case RIGHT -> baseX + width - lineWidth - element.offsetX();
        };
    }

    private int getTextWidth(String text) {
        return FONT_SERVICE.getWidth(CAT_FORMAT.format("{global}" + text));
    }

    private static class FeatureTextInfo {
        public final Feature Feature;
        public final Component formattedText;
        public final String rawText;
        public final int width;

        public FeatureTextInfo(Feature Feature, Component formattedText, String rawText, int width) {
            this.Feature = Feature;
            this.formattedText = formattedText;
            this.rawText = rawText;
            this.width = width;
        }
    }
}