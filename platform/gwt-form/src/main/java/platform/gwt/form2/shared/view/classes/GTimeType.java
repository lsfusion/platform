package platform.gwt.form2.shared.view.classes;

import com.google.gwt.i18n.client.DateTimeFormat;
import platform.gwt.form2.shared.view.grid.renderer.DateGridRenderer;
import platform.gwt.form2.shared.view.grid.renderer.GridCellRenderer;

public class GTimeType extends GDataType {
    public static GTimeType instance = new GTimeType();

    @Override
    public GridCellRenderer createGridCellRenderer() {
        return new DateGridRenderer(DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.TIME_MEDIUM));
    }
}
