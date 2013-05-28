package platform.gwt.sgwtbase.client.ui;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import java.util.Date;

public class DateCellFormatter implements CellFormatter {
    public static final DateCellFormatter instance = new DateCellFormatter();

    @Override
    public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
        if (value == null) {
            return null;
        }
        try {
            return DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_SHORT).format((Date)value);
        } catch (Exception e) {
            return value.toString();
        }
    }
}
