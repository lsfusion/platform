package lsfusion.gwt.form.shared.view.grid.renderer;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.base.client.EscapeUtils;
import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.changes.dto.GDateDTO;
import lsfusion.gwt.form.shared.view.changes.dto.GTimeDTO;

import java.util.Date;

public class DateGridCellRenderer extends TextBasedGridCellRenderer<Object> {
    private DateTimeFormat format;

    public DateGridCellRenderer(GPropertyDraw property, String pattern) {
        this(property, GwtSharedUtils.getDateFormat(pattern));
    }

    public DateGridCellRenderer(GPropertyDraw property, DateTimeFormat format) {
        super(property, Style.TextAlign.RIGHT);
        this.format = format;
    }

    public void setFormat(String pattern) {
        if(pattern != null)
            this.format = GwtSharedUtils.getDateFormat(pattern);
    }

    @Override
    protected String renderToString(Object value) {
        if (value instanceof GDateDTO) {
            return format.format(((GDateDTO) value).toDate());
        } else if (value instanceof GTimeDTO) {
            return format.format(((GTimeDTO) value).toTime());
        } else {
            return format.format((Date) value);
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
