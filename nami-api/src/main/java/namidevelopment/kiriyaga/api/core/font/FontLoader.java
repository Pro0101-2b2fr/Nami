package namidevelopment.kiriyaga.api.core.font;

import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.TrueTypeGlyphProvider;
import namidevelopment.kiriyaga.api.contract.FeatureContractService;
import namidevelopment.kiriyaga.api.contract.feature.FontFeatureConfig;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.GlyphStitcher;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import net.minecraft.client.gui.font.providers.GlyphProviderDefinition;
import net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition;
import net.minecraft.resources.Identifier;
import org.lwjgl.util.freetype.FreeType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import static namidevelopment.kiriyaga.api.NamiApi.*;

public class FontLoader {

    private FontSet storage;
    private int currentSize = -1;
    private int currentOversample = -1;
    private FontType lastFont = null;
    private int currentFlags = -1;
    private int currentRenderMode = -1;
    private float currentShiftX = Float.NaN;
    private float currentShiftY = Float.NaN;

    public void init() {
        FontFeatureConfig fontFeature = FeatureContractService.get(FontFeatureConfig.class);
        if (fontFeature == null) return;

        int newSize = fontFeature.getGlyphSize();
        int newOversample = fontFeature.getOversample();
        FontType selectedFont = fontFeature.getFontType();

        float shiftX = fontFeature.getShiftX();
        float shiftY = fontFeature.getShiftY();

        int renderMode = switch (fontFeature.getAntialiasMode()) {
          //  case MONO -> FreeType.FT_RENDER_MODE_MONO;
            case LIGHT -> FreeType.FT_RENDER_MODE_LIGHT;
            default -> FreeType.FT_RENDER_MODE_NORMAL;
        };

        int flags = FreeType.FT_LOAD_DEFAULT | (renderMode << 16);

        if (fontFeature.useAutoHint()) {
            flags |= FreeType.FT_LOAD_FORCE_AUTOHINT;
        }

        flags |= FreeType.FT_LOAD_NO_BITMAP;

        if (storage != null
                && currentSize == newSize
                && currentOversample == newOversample
                && selectedFont == lastFont
                && currentFlags == flags
                && currentRenderMode == renderMode
                && Float.compare(currentShiftX, shiftX) == 0
                && Float.compare(currentShiftY, shiftY) == 0
        ) return;

        NamiTrueTypeGlyphProviderDefinition loader = new NamiTrueTypeGlyphProviderDefinition(Identifier.fromNamespaceAndPath("nami-api", selectedFont.getFileName()), newSize, newOversample, shiftX, shiftY, "", flags, renderMode);

        try {
            GlyphProvider font = loader.unpack().orThrow().load(MC.getResourceManager());
            GlyphStitcher glyphBaker = new GlyphStitcher(MC.getTextureManager(), Identifier.fromNamespaceAndPath("nami-api", selectedFont.getFileName() + "_storage"));

            storage = new FontSet(glyphBaker);
            storage.reload(List.of(new GlyphProvider.Conditional(font, FontOption.Filter.ALWAYS_PASS)),
                    Collections.emptySet());

            currentSize = newSize;
            currentOversample = newOversample;
            currentFlags = flags;
            currentRenderMode = renderMode;
            currentShiftX = shiftX;
            currentShiftY = shiftY;
            lastFont = selectedFont;
        } catch (IOException e) {
            e.printStackTrace();
            storage = null;
            currentSize = -1;
            currentOversample = -1;
            lastFont = null;
            currentFlags = -1;
            currentRenderMode = -1;
            currentShiftX = Float.NaN;
            currentShiftY = Float.NaN;
        }
    }

    public FontSet getStorage() {
        return storage;
    }

    public int getCurrentSize() {
        return currentSize;
    }

    public int getCurrentOversample() {
        return currentOversample;
    }
}
