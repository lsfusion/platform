package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.classes.GTimeDTO;
import lsfusion.gwt.client.form.property.cell.classes.controller.TimeGridCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.DateCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.GridCellEditor;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

import java.text.ParseException;

import static lsfusion.gwt.client.base.GwtSharedUtils.getDefaultTimeFormat;
import static lsfusion.gwt.client.base.GwtSharedUtils.getDefaultTimeShortFormat;
import static lsfusion.gwt.client.classes.data.GDateType.parseDate;

public class GTimeType extends GFormatType<DateTimeFormat> {
    public static GTimeType instance = new GTimeType();

    @Override
    public DateTimeFormat getFormat(String pattern) {
        return GwtSharedUtils.getTimeFormat(pattern);
    }

    @Override
    public CellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new DateCellRenderer(property);
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new TimeGridCellEditor(editManager, editProperty);
    }

    @Override
    public GTimeDTO parseString(String value, String pattern) throws ParseException {
        return value.isEmpty() ? null : GTimeDTO.fromDate(parseDate(value, getDefaultTimeFormat(), getDefaultTimeShortFormat()));
    }

    @Override
    protected Object getDefaultWidthValue() {
        return GDateTimeType.getWideFormattableDateTime();
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeTimeCaption();
    }

    @Override
    public GEditBindingMap.EditEventFilter getEditEventFilter() {
        return GEditBindingMap.numberEventFilter;
    }
}
