package platform.gwt.view2.grid.renderer;

import com.google.gwt.cell.client.DateCell;
import com.google.gwt.i18n.client.DateTimeFormat;
import platform.gwt.utils.GwtSharedUtils;

import java.util.Date;

public class DateGridRenderer extends CellAdapterGridRenderer<Date> {
    public DateGridRenderer() {
        this(GwtSharedUtils.getDefaultDateFormat());
    }

    public DateGridRenderer(DateTimeFormat format) {
        super(new DateCell(format));
    }
}
