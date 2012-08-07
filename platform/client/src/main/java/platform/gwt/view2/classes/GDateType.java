package platform.gwt.view2.classes;

import platform.gwt.utils.GwtSharedUtils;
import platform.gwt.view2.GPropertyDraw;
import platform.gwt.view2.logics.FormLogicsProvider;
import platform.gwt.view2.panel.DatePanelRenderer;
import platform.gwt.view2.panel.PanelRenderer;
import platform.gwt.view2.grid.renderer.GridCellRenderer;
import platform.gwt.view2.grid.renderer.DateGridRenderer;

public class GDateType extends GDataType {
    public static GType instance = new GDateType();

    @Override
    public PanelRenderer createPanelRenderer(FormLogicsProvider formLogics, GPropertyDraw property) {
        return new DatePanelRenderer(property, GwtSharedUtils.getDefaultDateFormat());
    }

    @Override
    public GridCellRenderer createGridCellRenderer() {
        return new DateGridRenderer();
    }
}
