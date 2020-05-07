package lsfusion.gwt.client.view;

public enum GColorTheme {
    LIGHT("light", null), DARK("dark", 0.33f);
    
    public final static GColorTheme DEFAULT = LIGHT; 

    private final String sid;
    private final Float colorInvertFactor;

    GColorTheme(String sid, Float invertFactor) {
        this.sid = sid;
        this.colorInvertFactor = invertFactor;
    }

    public String getSid() {
        return sid;
    }
    
    public Float getColorInvertFactor() {
        return colorInvertFactor;
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
}
