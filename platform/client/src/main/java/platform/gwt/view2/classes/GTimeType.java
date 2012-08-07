package platform.gwt.view2.classes;

import com.google.gwt.i18n.client.DateTimeFormat;
import platform.gwt.view2.GPropertyDraw;
import platform.gwt.view2.grid.renderer.DateGridRenderer;
import platform.gwt.view2.grid.renderer.GridCellRenderer;
import platform.gwt.view2.logics.FormLogicsProvider;
import platform.gwt.view2.panel.DatePanelRenderer;
import platform.gwt.view2.panel.PanelRenderer;

public class GTimeType extends GDataType {
    public static GType instance = new GTimeType();

    @Override
    public GridCellRenderer createGridCellRenderer() {
        return new DateGridRenderer(DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.TIME_MEDIUM));
    }

    @Override
    public PanelRenderer createPanelRenderer(FormLogicsProvider formLogics, GPropertyDraw property) {
        return new DatePanelRenderer(property, DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.TIME_MEDIUM));
    }
}
