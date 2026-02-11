package namidevelopment.kiriyaga.api.util;

import namidevelopment.kiriyaga.api.contract.FeatureContractService;
import namidevelopment.kiriyaga.api.contract.feature.ColorFeatureConfig;

import java.awt.Color;

public class CatStyles {

    public Color global() {
        Color gc = FeatureContractService.get(ColorFeatureConfig.class).getStyledGlobalColor();
        return new Color(gc.getRed(), gc.getGreen(), gc.getBlue(), 255);
    }

    public Color friend() {
        Color gc = FeatureContractService.get(ColorFeatureConfig.class).getFriendColor();
        return new Color(gc.getRed(), gc.getGreen(), gc.getBlue(), 255);
    }

    public Color enemy() {
        Color gc = FeatureContractService.get(ColorFeatureConfig.class).getEnemyColor();
        return new Color(gc.getRed(), gc.getGreen(), gc.getBlue(), 255);
    }

    public Color secondary() {
        Color gs = FeatureContractService.get(ColorFeatureConfig.class).getStyledSecondColor();
        return new Color(gs.getRed(), gs.getGreen(), gs.getBlue(), 255);
    }

    // color schemes here https://htmlcolorcodes.com/minecraft-color-codes/

    public static Color black() {
        return new Color(0x000000);
    }

    public static Color darkBlue() {
        return new Color(0x0000AA);
    }

    public static Color darkGreen() {
        return new Color(0x00AA00);
    }

    public static Color darkAqua() {
        return new Color(0x00AAAA);
    }

    public static Color darkRed() {
        return new Color(0xAA0000);
    }

    public static Color darkPurple() {
        return new Color(0xAA00AA);
    }

    public static Color gold() {
        return new Color(0xFFAA00);
    }

    public static Color gray() {
        return new Color(0xAAAAAA);
    }

    public static Color darkGray() {
        return new Color(0x555555);
    }

    public static Color blue() {
        return new Color(0x5555FF);
    }

    public static Color green() {
        return new Color(0x55FF55);
    }

    public static Color aqua() {
        return new Color(0x55FFFF);
    }

    public static Color red() {
        return new Color(0xFF5555);
    }

    public static Color lightPurple() {
        return new Color(0xFF55FF);
    }

    public static Color yellow() {
        return new Color(0xFFFF55);
    }

    public static Color white() {
        return new Color(0xFFFFFF);
    }
}
