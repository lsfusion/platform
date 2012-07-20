package platform.gwt.view2.classes;

import platform.gwt.view2.GPropertyDraw;
import platform.gwt.view2.logics.FormLogicsProvider;
import platform.gwt.view2.panel.ColorPanelRenderer;
import platform.gwt.view2.panel.PanelRenderer;
import platform.gwt.view2.grid.renderer.GridCellRenderer;
import platform.gwt.view2.grid.renderer.ColorGridRenderer;

public class GColorType extends GDataType {
    public static GType instance = new GColorType();

    @Override
    public PanelRenderer createPanelRenderer(FormLogicsProvider formLogics, GPropertyDraw property) {
        return new ColorPanelRenderer(property);
    }

    @Override
    public GridCellRenderer createGridCellRenderer() {
        return new ColorGridRenderer();
    }
}
