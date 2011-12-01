package platform.interop;

public class ToolbarPanelLocation extends PanelLocation {
    public boolean isShortcutLocation() {
        return false;
    }

    public boolean isToolbarLocation() {
        return true;
    }
}
