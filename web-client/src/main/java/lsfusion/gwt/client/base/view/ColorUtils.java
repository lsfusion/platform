package lsfusion.gwt.client.base.view;

import lsfusion.gwt.client.base.GwtClientUtils;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static lsfusion.gwt.client.view.MainFrame.colorTheme;

public class ColorUtils {
    public static int toRGB(String color) {
        if (color != null) {
            try {
                return Integer.decode(color);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    public static String toHexString(int colorComponent) {
        String c = Integer.toHexString(colorComponent);
        if (c.length() < 2) {
            c = GwtClientUtils.replicate('0', 2 - c.length()) + c;
        }
        return c;
    }

    public static String toColorString(int iR, int iG, int iB) {
        return "#" + toHexString(iR) + toHexString(iG) + toHexString(iB);
    }

    public static String toColorString(int iColor) {
        return toColorString(getRed(iColor), getGreen(iColor), getBlue(iColor));
    }

    public static String mixColors(String color1, String color2) {
        int rgb1 = toRGB(color1);
        int rgb2 = toRGB(color2);
        if (rgb1 == -1) {
            return rgb2 != -1 ? color2 : null;
        } else if (rgb2 == -1) {
            return color1;
        }
        return toColorString(rgb1 & rgb2);
    }

    public static int getRed(String color) {
        return getRed(Integer.decode(color));
    }
    
    public static int getRed(int color) {
        return (color >> 16) & 0xFF;
    }

    public static int getGreen(String color) {
        return getGreen(Integer.decode(color));
    }
    
    public static int getGreen(int color) {
        return (color >> 8) & 0xFF;
    }

    public static int getBlue(String color) {
        return getBlue(Integer.decode(color));
    }
    
    public static int getBlue(int color) {
        return color & 0xFF;
    }

    // based on java.awt.Color's darker() 
    public static String getDisplayBackground(String baseColor) {
        Float invertFactor = colorTheme.getColorInvertFactor();
        if (baseColor != null && invertFactor != null) {
            return toColorString(max((int) (getRed(baseColor) * invertFactor), 0),
                    max((int) (getGreen(baseColor) * invertFactor), 0),
                    max((int) (getBlue(baseColor) * invertFactor), 0));
        }
        return baseColor;
    }

    // based on java.awt.Color's brighter() 
    public static String getDisplayForeground(String baseColor) {
        Float invertFactor = colorTheme.getColorInvertFactor();
        if (baseColor != null && invertFactor != null) {
            int r = getRed(baseColor);
            int g = getGreen(baseColor);
            int b = getBlue(baseColor);

            int i = (int) (1.0 / (1.0 - invertFactor));
            if (r == 0 && g == 0 && b == 0) {
                return toColorString(i, i, i);
            }
            if (r > 0 && r < i) r = i;
            if (g > 0 && g < i) g = i;
            if (b > 0 && b < i) b = i;

            return toColorString(min((int) (r / invertFactor), 255),
                    min((int) (g / invertFactor), 255),
                    min((int) (b / invertFactor), 255));
        }
        return baseColor;
    }
}
