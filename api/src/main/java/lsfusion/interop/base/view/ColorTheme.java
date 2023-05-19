package lsfusion.interop.base.view;

import lsfusion.base.BaseUtils;

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
        return BaseUtils.addSuffix(path, getFileNameSuffix());
    }
    public static String addIDSuffix(String ID, String suffix) {
        return ID + suffix;
    }
    public String getID(String ID) {
        return addIDSuffix(ID, getFileNameSuffix());
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
