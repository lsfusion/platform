package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.GDateDTO;
import lsfusion.gwt.client.form.property.cell.classes.GTimeDTO;

import java.util.Date;

public class DateGridCellRenderer extends FormatGridCellRenderer<Object, DateTimeFormat> {
    public DateGridCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected String castToString(Object value) {
        if (value instanceof GDateDTO) {
            return format.format(((GDateDTO) value).toDate());
        } else if (value instanceof GTimeDTO) {
            return format.format(((GTimeDTO) value).toTime());
        } else {
            return format.format((Date) value);
        }
    }
}
