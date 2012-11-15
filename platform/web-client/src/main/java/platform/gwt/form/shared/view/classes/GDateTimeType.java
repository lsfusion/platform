package platform.gwt.form.shared.view.classes;

import com.google.gwt.i18n.client.DateTimeFormat;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.renderer.DateGridRenderer;
import platform.gwt.form.shared.view.grid.renderer.GridCellRenderer;

public class GDateTimeType extends GDataType {
    public static GDateTimeType instance = new GDateTimeType();

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new DateGridRenderer(DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_TIME_MEDIUM));
    }

    @Override
    public String getPreferredMask() {
        return "01.01.2001 00:00:00";
    }
}
