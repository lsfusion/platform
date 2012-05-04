package platform.server.logics.panellocation;

import platform.server.form.view.panellocation.PanelLocationView;

public abstract class PanelLocation {
    public abstract boolean isShortcutLocation();
    public abstract boolean isToolbarLocation();
    public abstract PanelLocationView convertToView();
}