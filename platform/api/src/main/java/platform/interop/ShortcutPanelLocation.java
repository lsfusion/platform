package platform.interop;

public class ShortcutPanelLocation extends PanelLocation {
    protected String onlyPropertySID;

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
}
