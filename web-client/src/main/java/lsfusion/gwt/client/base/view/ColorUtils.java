package lsfusion.gwt.client.base.view;

import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.view.StyleDefaults;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static lsfusion.gwt.client.view.MainFrame.colorTheme;

public class ColorUtils {
    public static int toRGB(String color) {
        if (color != null) {
            try {
                //transform short form to full form: #00f -> #0000ff
                if(color.startsWith("#") && color.length() == 4) {
                    color = "#" + color.charAt(1) + color.charAt(1)
                            + color.charAt(2) + color.charAt(2)
                            + color.charAt(3) + color.charAt(3);
                }
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
            int newBackgroundColor = toRGB(StyleDefaults.getComponentBackground());
            int customLimitColor = toRGB(StyleDefaults.getTextColor());

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
    

    public static String darkenColor(String color) {
        int rgb = toRGB(color);
        int r = getRed(rgb);
        int g = getGreen(rgb);
        int b = getBlue(rgb);
        double opacity = Math.max((255f - Math.min(Math.min(r, g), b)) / 255, 0.5);
        return rgbToRgba(getThemedColor(darkenComp(r, opacity), darkenComp(g, opacity), darkenComp(b, opacity)), opacity);
    }

    private static int darkenComp(int comp, double opacity) {
        int darken = (int) ((comp - 255 * opacity) / (1 - opacity));
        return darken > 0 ? darken : (int) (comp * opacity);
    }

    private static String rgbToRgba(String rgb, double a) {
        int color = toRGB(rgb);
        return "rgba(" + getRed(color) + ", " + getGreen(color) + ", " + getBlue(color) + ", " + a + ")";
    }

    //copy of FontInfoConverter.convertToFontInfo
    public static GFont convertToFontInfo(String value) {
        if(value == null)
            return null;

        String name = null;

        // Название шрифта состоит из нескольких слов
        if(value.contains("\"")) {
            int start = value.indexOf('"');
            int end = value.indexOf('"', start + 1) + 1;
            name = value.substring(start + 1, end - 1);
            value = value.substring(0, start) + value.substring(end);
        }

        int size = 0;
        boolean bold = false;
        boolean italic = false;
        for (String part : value.split(" ")) {
            if (part.isEmpty()) {
                continue;
            }

            if (part.equalsIgnoreCase("italic")) {
                italic = true;
            } else if (part.equalsIgnoreCase("bold")) {
                bold = true;
            } else {
                int sz = toInt(part, -1);
                if (sz != -1) {
                    //числовой токен

                    if (sz <= 0) {
                        throw new RuntimeException("Size must be > 0");
                    }
                    if (size != 0) {
                        //уже просетали size
                        throw new RuntimeException("Incorrect format: several number tokens specified");
                    }

                    size = sz;
                } else {
                    //текстовый токен
                    if (name != null) {
                        //уже просетали name
                        throw new RuntimeException("Incorrect format: several name tokens specified");
                    }

                    name = part;
                }
            }
        }

        return new GFont(name, size, bold, italic);
    }

    //copy from NumberUtils that is not available in gwt
    private static int toInt(final String str, final int defaultValue) {
        if(str == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(str);
        } catch (final NumberFormatException nfe) {
            return defaultValue;
        }
    }

}
