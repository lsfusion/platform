package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.GDateDTO;
import lsfusion.gwt.client.form.property.cell.classes.GDateTimeDTO;
import lsfusion.gwt.client.form.property.cell.classes.GTimeDTO;
import lsfusion.gwt.client.form.property.cell.classes.GZDateTimeDTO;

public class DateGridCellRenderer extends FormatGridCellRenderer<Object, DateTimeFormat> {
    public DateGridCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    public String format(Object value) {
        if (value instanceof GDateDTO) {
            return format.format(((GDateDTO) value).toDate());
        } else if (value instanceof GTimeDTO) {
            return format.format(((GTimeDTO) value).toTime());
        } else if(value instanceof GDateTimeDTO) {
            return format.format(((GDateTimeDTO) value).toDateTime());
        } else {
            return format.format(((GZDateTimeDTO) value).toDateTime());
        }
    }
}
