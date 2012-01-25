package platform.interop;

public class ShortcutPanelLocation extends PanelLocation {
    protected String onlyPropertySID;
    protected boolean defaultOne;

    public ShortcutPanelLocation() {}

    public ShortcutPanelLocation(String onlyPropertySID) {
        this.onlyPropertySID = onlyPropertySID;
    }

    public boolean isShortcutLocation() {
        return true;
    }

    public boolean isToolbarLocation() {
        return false;
    }

    public void setOnlyPropertySID(String sID) {
        onlyPropertySID = sID;
    }

    public String getOnlyPropertySID() {
        return onlyPropertySID;
    }

    public void setDefault(boolean defaultOne) {
        this.defaultOne = defaultOne;
    }

    public boolean isDefault() {
        return defaultOne;
    }
}
