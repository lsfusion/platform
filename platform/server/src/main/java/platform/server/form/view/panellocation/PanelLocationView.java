package platform.server.form.view.panellocation;

import platform.server.serialization.ServerCustomSerializable;

public abstract class PanelLocationView implements ServerCustomSerializable {
    public abstract boolean isShortcutLocation();
    public abstract boolean isToolbarLocation();
}
