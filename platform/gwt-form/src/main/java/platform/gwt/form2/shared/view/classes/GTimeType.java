package platform.gwt.form2.shared.view.classes;

import com.google.gwt.i18n.client.DateTimeFormat;
import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.grid.renderer.DateGridRenderer;
import platform.gwt.form2.shared.view.grid.renderer.GridCellRenderer;

public class GTimeType extends GDataType {
    public static GTimeType instance = new GTimeType();

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new DateGridRenderer(DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.TIME_MEDIUM));
    }

    @Override
    public String getPreferredMask() {
        return "00:00:00";
    }
}
