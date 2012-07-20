package platform.gwt.view2.classes;

import platform.gwt.view2.GPropertyDraw;
import platform.gwt.view2.logics.FormLogicsProvider;
import platform.gwt.view2.panel.LogicalPanelRenderer;
import platform.gwt.view2.panel.PanelRenderer;
import platform.gwt.view2.grid.renderer.GridCellRenderer;
import platform.gwt.view2.grid.renderer.LogicalGridRenderer;

public class GLogicalType extends GDataType {
    public static GType instance = new GLogicalType();

    @Override
    public PanelRenderer createPanelRenderer(FormLogicsProvider formLogics, GPropertyDraw property) {
        return new LogicalPanelRenderer(property);
    }

    @Override
    public GridCellRenderer createGridCellRenderer() {
        return new LogicalGridRenderer();
    }
}
