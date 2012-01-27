package platform.server.logics.panellocation;

import platform.server.form.view.panellocation.PanelLocationView;
import platform.server.form.view.panellocation.ToolbarPanelLocationView;

public class ToolbarPanelLocation extends PanelLocation {
    public boolean isShortcutLocation() {
        return false;
    }

    public boolean isToolbarLocation() {
        return true;
    }

    @Override
    public PanelLocationView convertToView() {
        return new ToolbarPanelLocationView();
    }
}
