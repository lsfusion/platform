package platform.gwt.view2.classes;

import platform.gwt.view2.GPropertyDraw;
import platform.gwt.view2.logics.FormLogicsProvider;
import platform.gwt.view2.panel.ActionPanelRenderer;
import platform.gwt.view2.panel.PanelRenderer;
import platform.gwt.view2.grid.renderer.GridCellRenderer;
import platform.gwt.view2.grid.renderer.ActionGridRenderer;

public class GActionType extends GDataType {
    public static GType instance = new GActionType();

    @Override
    public PanelRenderer createPanelRenderer(FormLogicsProvider formLogics, GPropertyDraw property) {
        return new ActionPanelRenderer(property);
    }

    @Override
    public GridCellRenderer createGridCellRenderer() {
        return new ActionGridRenderer();
    }
}
