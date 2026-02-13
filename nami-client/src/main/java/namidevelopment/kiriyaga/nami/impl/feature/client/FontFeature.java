package namidevelopment.kiriyaga.nami.impl.feature.client;

import it.unimi.dsi.fastutil.doubles.DoubleSet;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.contract.feature.FontFeatureConfig;
import namidevelopment.kiriyaga.api.core.font.FontType;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import namidevelopment.kiriyaga.api.model.setting.DoubleSetting;
import namidevelopment.kiriyaga.api.model.setting.EnumSetting;
import namidevelopment.kiriyaga.api.model.setting.IntSetting;

@RegisterFeature
public class FontFeature extends Feature implements FontFeatureConfig {

    public final EnumSetting<FontType> fontType = addSetting(new EnumSetting<>("Font", FontType.ARIAL));
    public final IntSetting shadowDarken = addSetting(new IntSetting("ShadowDarken", 70, 60, 85));
    public final IntSetting glyphSize = addSetting(new IntSetting("Size", 10, 6, 24));
    public final IntSetting oversample = addSetting(new IntSetting("Oversample", 2, 2, 8));
    public final DoubleSetting shiftX = addSetting(new DoubleSetting("ShiftX", 0.0f, -5.0f, 5.0f));
    public final DoubleSetting shiftY = addSetting(new DoubleSetting("ShiftY", 1.0f, -5.0f, 5.0f));
    public final EnumSetting<FontAntialiasMode> antialiasMode = addSetting(new EnumSetting<>("Antialias", FontAntialiasMode.NORMAL));
    public final BoolSetting autoHint = addSetting(new BoolSetting("AutoHint", true));

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
    public float getShiftX() {
        return shiftX.get().floatValue();
    }

    @Override
    public float getShiftY() {
        return shiftY.get().floatValue();
    }

    @Override
    public FontAntialiasMode getAntialiasMode() {
        return antialiasMode.get();
    }

    @Override
    public boolean useAutoHint() {
        return autoHint.get();
    }

    @Override
    public int getShadowDarken() {
        return shadowDarken.get();
    }
}
