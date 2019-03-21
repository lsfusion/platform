package lsfusion.gwt.shared.view.classes;

import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.GridCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.TimeGridCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.DateGridCellRenderer;
import lsfusion.gwt.client.form.property.cell.GridCellRenderer;
import lsfusion.gwt.shared.GwtSharedUtils;
import lsfusion.gwt.shared.view.GEditBindingMap;
import lsfusion.gwt.shared.view.GPropertyDraw;
import lsfusion.gwt.shared.view.changes.dto.GTimeDTO;

import java.text.ParseException;

import static lsfusion.gwt.shared.GwtSharedUtils.getDefaultTimeFormat;
import static lsfusion.gwt.shared.GwtSharedUtils.getDefaultTimeShortFormat;
import static lsfusion.gwt.shared.view.classes.GDateType.parseDate;

public class GTimeType extends GFormatType<DateTimeFormat> {
    public static GTimeType instance = new GTimeType();

    @Override
    public DateTimeFormat getFormat(String pattern) {
        return GwtSharedUtils.getTimeFormat(pattern);
    }

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new DateGridCellRenderer(property);
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
