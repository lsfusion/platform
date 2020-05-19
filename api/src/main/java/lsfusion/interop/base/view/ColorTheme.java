package lsfusion.interop.base.view;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.max;
import static java.lang.Math.min;

public enum ColorTheme {
    LIGHT("light", null), DARK("dark", 0.33f);
    
    public final static ColorTheme DEFAULT = LIGHT;

    private final String sid;
    private final Float colorInvertFactor;

    ColorTheme(String sid, Float invertFactor) {
        this.sid = sid;
        this.colorInvertFactor = invertFactor;
    }

    public String getSid() {
        return sid;
    }

    public boolean isDefault() {
        return this == DEFAULT;
    }
    
    public boolean isLight() {
        return this == LIGHT;
    }
    
    public String getFileNameSuffix() {
        return isDefault() ? "" : "_" + getSid();
    }

    public String getImagePath(String path) {
        if (path != null) {
            int dotInd = path.lastIndexOf(".");
            if (dotInd != -1) {
                return path.substring(0, dotInd) + getFileNameSuffix() + path.substring(dotInd);
            }
        }

        return path;
    }

    private static final Map<String, ColorTheme> lookup = new HashMap<>();

    static {
        for (ColorTheme env : ColorTheme.values()) {
            lookup.put(env.getSid(), env);
        }
    }

    public static ColorTheme get(String sid) {
        return lookup.get(sid);
    }
    
    // based on java.awt.Color's darker() 
    public Color getDisplayBackground(Color baseColor) {
        if (baseColor != null && colorInvertFactor != null) {
            return new Color(max((int) (baseColor.getRed() * colorInvertFactor), 0),
                    max((int) (baseColor.getGreen() * colorInvertFactor), 0),
                    max((int) (baseColor.getBlue() * colorInvertFactor), 0),
                    baseColor.getAlpha());
        }
        return baseColor;
    }

    // based on java.awt.Color's brighter() 
    public Color getDisplayForeground(Color baseColor) {
        if (baseColor != null && colorInvertFactor != null) {
            int r = baseColor.getRed();
            int g = baseColor.getGreen();
            int b = baseColor.getBlue();
            int alpha = baseColor.getAlpha();

            int i = (int) (1.0 / (1.0 - colorInvertFactor));
            if (r == 0 && g == 0 && b == 0) {
                return new Color(i, i, i, alpha);
            }
            if (r > 0 && r < i) r = i;
            if (g > 0 && g < i) g = i;
            if (b > 0 && b < i) b = i;

            return new Color(min((int) (r / colorInvertFactor), 255),
                    min((int) (g / colorInvertFactor), 255),
                    min((int) (b / colorInvertFactor), 255),
                    alpha);
        }
        return baseColor;
    }
}
