package namidevelopment.kiriyaga.nami.impl.feature.client;

import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.contract.feature.FontFeatureConfig;
import namidevelopment.kiriyaga.api.core.font.FontType;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;

@RegisterFeature
public class FontFeature extends Feature implements FontFeatureConfig {

    public final EnumSetting<FontType> fontType = addSetting(new EnumSetting<>("Font", FontType.ARIAL));
    public final IntSetting shadowDarken = addSetting(new IntSetting("ShadowDarken", 70, 60, 85));
    public final IntSetting glyphSize = addSetting(new IntSetting("Size", 10, 6, 24));
    public final IntSetting oversample = addSetting(new IntSetting("Oversample", 2, 2, 8));

    public FontFeature() {
        super("Font", "Custom font renderer.", FeatureCategory.of("Client"), "f", "customfont");
    }

    @Override
    public FontType getFontType() {
        return fontType.get();
    }

    @Override
    public int getGlyphSize() {
        return glyphSize.get();
    }

    @Override
    public int getOversample() {
        return oversample.get();
    }

    @Override
    public int getShadowDarken() {
        return shadowDarken.get();
    }
}
