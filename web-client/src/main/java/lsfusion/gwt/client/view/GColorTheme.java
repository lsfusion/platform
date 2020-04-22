package lsfusion.gwt.client.view;

public enum GColorTheme {
    LIGHT("light"), DARK("dark");
    
    public final static GColorTheme DEFAULT = LIGHT; 

    private String sid;

    GColorTheme(String sid) {
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
}
