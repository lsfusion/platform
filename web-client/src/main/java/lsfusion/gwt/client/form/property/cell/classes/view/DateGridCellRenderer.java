package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.GDateDTO;
import lsfusion.gwt.client.form.property.cell.classes.GDateTimeDTO;
import lsfusion.gwt.client.form.property.cell.classes.GTimeDTO;

public class DateGridCellRenderer extends FormatGridCellRenderer<Object, DateTimeFormat> {

    public DateGridCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected String renderToString(Object value) {
        if (value instanceof GDateDTO) {
            return format.format(((GDateDTO) value).toDate());
        } else if (value instanceof GTimeDTO) {
            return format.format(((GTimeDTO) value).toTime());
        } else {
            return format.format(((GDateTimeDTO) value).toDateTime());
        }
    }

    @Override
    protected void setInnerText(DivElement div, String innerText) {
        if (innerText == null) {
            if (property.isEditableNotNull()) {
                div.setInnerText(REQUIRED_VALUE);
                div.addClassName("requiredValueString");
            } else {
                div.setInnerText(EscapeUtils.UNICODE_NBSP);
                div.removeClassName("requiredValueString");
            }
        } else {
            div.setInnerText(innerText);
            div.removeClassName("requiredValueString");
        }
    }
}
