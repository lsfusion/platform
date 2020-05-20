package lsfusion.interop.base.view;

import java.util.HashMap;
import java.util.Map;

public enum ColorTheme {
    LIGHT("light"), DARK("dark");
    
    public final static ColorTheme DEFAULT = LIGHT;

    private final String sid;

    ColorTheme(String sid) {
        this.sid = sid;
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
}
