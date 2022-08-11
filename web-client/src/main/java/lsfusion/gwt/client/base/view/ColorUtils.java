package lsfusion.gwt.client.base.view;

import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.view.StyleDefaults;

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
            c = GwtSharedUtils.replicate('0', 2 - c.length()) + c;
        }
        return c;
    }

    public static String toColorString(int iR, int iG, int iB) {
        return "#" + toHexString(iR) + toHexString(iG) + toHexString(iB);
    }

    public static String toColorString(int iColor) {
        return toColorString(getRed(iColor), getGreen(iColor), getBlue(iColor));
    }

    public static int getRed(int color) {
        return (color >> 16) & 0xFF;
    }

    public static int getGreen(int color) {
        return (color >> 8) & 0xFF;
    }

    public static int getBlue(int color) {
        return color & 0xFF;
    }

    public static String getThemedColor(String baseColor) {
        if (!colorTheme.isDefault() && baseColor != null) {
            int baseRGB = toRGB(baseColor);
            return getThemedColor(getRed(baseRGB), getGreen(baseRGB), getBlue(baseRGB));
        }
        return baseColor;
    }

    public static String getThemedColor(int baseRed, int baseGreen, int baseBlue) {
        if (!colorTheme.isDefault()) {
            int baseBackgroundColor = toRGB(StyleDefaults.getDefaultComponentBackground());
            int newBackgroundColor = toRGB(StyleDefaults.getComponentBackground(colorTheme));
            int customLimitColor = toRGB(StyleDefaults.getTextColor(colorTheme));

            float[] hsb = RGBtoHSB(
                    max(min(getRed(baseBackgroundColor) - baseRed + getRed(newBackgroundColor), getRed(customLimitColor)), 0),
                    max(min(getGreen(baseBackgroundColor) - baseGreen + getGreen(newBackgroundColor), getGreen(customLimitColor)), 0),
                    max(min(getBlue(baseBackgroundColor) - baseBlue + getBlue(newBackgroundColor), getBlue(customLimitColor)), 0));
            return toColorString(HSBtoRGB(Math.abs(0.5f + hsb[0]), hsb[1], hsb[2]));
        }
        return toColorString(baseRed, baseGreen, baseBlue);
    }

    // c/p of java.awt.Color's HSBtoRGB() 
    public static int HSBtoRGB(float hue, float saturation, float brightness) {
        int r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float)Math.floor(hue)) * 6.0f;
            float f = h - (float)java.lang.Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
                case 0:
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (t * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                    break;
                case 1:
                    r = (int) (q * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                    break;
                case 2:
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (t * 255.0f + 0.5f);
                    break;
                case 3:
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (q * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                    break;
                case 4:
                    r = (int) (t * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                    break;
                case 5:
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (q * 255.0f + 0.5f);
                    break;
            }
        }
        return 0xff000000 | (r << 16) | (g << 8) | (b << 0);
    }

    // c/p of java.awt.Color's RGBtoHSB() 
    public static float[] RGBtoHSB(int r, int g, int b) {
        float hue, saturation, brightness;
        float[] hsbvals = new float[3];
        int cmax = (r > g) ? r : g;
        if (b > cmax) cmax = b;
        int cmin = (r < g) ? r : g;
        if (b < cmin) cmin = b;

        brightness = ((float) cmax) / 255.0f;
        if (cmax != 0)
            saturation = ((float) (cmax - cmin)) / ((float) cmax);
        else
            saturation = 0;
        if (saturation == 0)
            hue = 0;
        else {
            float redc = ((float) (cmax - r)) / ((float) (cmax - cmin));
            float greenc = ((float) (cmax - g)) / ((float) (cmax - cmin));
            float bluec = ((float) (cmax - b)) / ((float) (cmax - cmin));
            if (r == cmax)
                hue = bluec - greenc;
            else if (g == cmax)
                hue = 2.0f + redc - bluec;
            else
                hue = 4.0f + greenc - redc;
            hue = hue / 6.0f;
            if (hue < 0)
                hue = hue + 1.0f;
        }
        hsbvals[0] = hue;
        hsbvals[1] = saturation;
        hsbvals[2] = brightness;
        return hsbvals;
    }

    public static String rgbToArgb(String rgb) {
        return rgb.replace("#", "FF");
    }
    

    public static String correctSB(String color, float saturation_factor, float brightness_factor) {
        int rgb = toRGB(color);
        float[] hsb = RGBtoHSB(getRed(rgb), getGreen(rgb), getBlue(rgb));
        return toColorString(HSBtoRGB(
                hsb[0], 
                max(min(hsb[1] * saturation_factor, 1.0f), 0), 
                max(min(hsb[2] * brightness_factor, 1.0f), 0))
        );
    }
}
