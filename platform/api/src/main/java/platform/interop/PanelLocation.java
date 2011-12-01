package platform.interop;

import java.io.Serializable;

public abstract class PanelLocation implements Serializable {
    public abstract boolean isShortcutLocation();
    public abstract boolean isToolbarLocation();
}
