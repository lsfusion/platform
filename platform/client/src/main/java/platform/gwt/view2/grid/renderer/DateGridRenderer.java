package platform.gwt.view2.grid.renderer;

import com.google.gwt.cell.client.DateCell;
import platform.gwt.utils.GwtSharedUtils;

import java.util.Date;

public class DateGridRenderer extends CellAdapterGridRenderer<Date> {
    public DateGridRenderer() {
        super(new DateCell(GwtSharedUtils.getDefaultDateFormat()));
    }
}
