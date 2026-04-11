package namidevelopment.kiriyaga.api.util;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.ArrayList;
import java.util.List;

public class KeyUtils {

    private static final int MAX_KEY = 350;
    private static final int MAX_MOUSE = 8;

    public static int parseKey(String keyName) {
        if (keyName == null || keyName.equalsIgnoreCase("NONE"))
            return -1;

        keyName = keyName.toUpperCase();

        try {
            return Integer.parseInt(keyName);
        } catch (NumberFormatException ignored) {
        }

        for (int key = 0; key <= MAX_KEY; key++) {
            String name = getKeyName(key);
            if (name.equalsIgnoreCase(keyName)) {
                return key;
            }
        }
        for (int button = 0; button <= MAX_MOUSE; button++) {
            String name = getMouseName(button);
            if (name.equalsIgnoreCase(keyName)) {
                return button;
            }
        }
        return -1;
    }

    public static String getKeyName(int keyCode) {
        if (keyCode == -1)
            return "NONE";

        InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(keyCode);
        String name = key.getName();
        if (name.startsWith("key.keyboard.")) {
            return name.replace("key.keyboard.", "").toUpperCase();
        }
        return name.toUpperCase();
    }

    public static String getMouseName(int button) {
        InputConstants.Key key = InputConstants.Type.MOUSE.getOrCreate(button);
        String name = key.getName();
        if (name.startsWith("key.mouse.")) {
            return name.replace("key.mouse.", "MOUSE").toUpperCase();
        }
        return name.toUpperCase();
    }

    public static List<String> getAllKeyNames() {
        List<String> list = new ArrayList<>();
        list.add("NONE");
        for (int key = 0; key <= MAX_KEY; key++) {
            list.add(getKeyName(key));
        }

        for (int button = 0; button <= MAX_MOUSE; button++) {
            list.add(getMouseName(button));
        }
        return list;
    }

    public static boolean isDigit(int keyCode) {
        return (keyCode >= 48 && keyCode <= 57) || (keyCode >= 320 && keyCode <= 329);
    }

    public static String getDigit(int keyCode) {
        if (keyCode >= 48 && keyCode <= 57) {
            return String.valueOf((char) keyCode);
        }
        if (keyCode >= 320 && keyCode <= 329) {
            return String.valueOf(keyCode - 320);
        }
        return "";
    }

    public static boolean isValidHex(int keyCode) {
        return isDigit(keyCode) || (keyCode >= 65 && keyCode <= 70);
    }

    public static String getHexChar(int keyCode) {
        if (isDigit(keyCode)) {
            return getDigit(keyCode);
        }
        if (keyCode >= 65 && keyCode <= 70) {
            return String.valueOf((char) keyCode);
        }
        return "";
    }

    public static boolean isDecimal(int keyCode) {
        return keyCode == 46 || keyCode == 330 || keyCode == 44 || keyCode == 59;
    }

    public static boolean isMinus(int keyCode) {
        return keyCode == 45 || keyCode == 333;
    }

    public static boolean isModifier(int keyCode) {
        return (keyCode >= 340 && keyCode <= 348) || keyCode == 280 || keyCode == 281 || keyCode == 282;
    }
}