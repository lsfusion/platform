package lsfusion.gwt.client.view;

public enum GColorTheme {
    LIGHT("light", null, "static/css/theme/light.css"), DARK("dark", 0.33f, "static/css/theme/dark.css");

    public final static GColorTheme DEFAULT = LIGHT; 

    private final String sid;
    private final Float colorInvertFactor;
    private final String url;

    GColorTheme(String sid, Float invertFactor, String url) {
        this.sid = sid;
        this.colorInvertFactor = invertFactor;
        this.url = url;
    }

    public String getSid() {
        return sid;
    }

    public String getUrl() {
        return url;
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
        assert !isDefault();
        int dotInd = path.lastIndexOf(".");
        if (dotInd != -1) {
            return path.substring(0, dotInd) + getFileNameSuffix() + path.substring(dotInd);
        }
        return path;
    }
}
