package platform.gwt.view2.classes;

import platform.gwt.view2.GPropertyDraw;
import platform.gwt.view2.logics.FormLogicsProvider;
import platform.gwt.view2.panel.PanelRenderer;
import platform.gwt.view2.grid.renderer.GridCellRenderer;
import platform.gwt.view2.grid.renderer.NumberGridRenderer;

public class GObjectType extends GType {
    public static final GObjectType instance = new GObjectType();

    @Override
    public PanelRenderer createPanelRenderer(FormLogicsProvider formLogics, GPropertyDraw property) {
        return super.createPanelRenderer(formLogics, property);
    }

    @Override
    public GridCellRenderer createGridCellRenderer() {
        return new NumberGridRenderer();
    }
}
