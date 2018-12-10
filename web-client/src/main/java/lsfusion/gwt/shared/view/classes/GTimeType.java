package lsfusion.gwt.shared.view.classes;

import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.client.form.MainFrameMessages;
import lsfusion.gwt.client.grid.EditManager;
import lsfusion.gwt.client.grid.editor.GridCellEditor;
import lsfusion.gwt.client.grid.editor.TimeGridCellEditor;
import lsfusion.gwt.client.grid.renderer.DateGridCellRenderer;
import lsfusion.gwt.client.grid.renderer.GridCellRenderer;
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
        return MainFrameMessages.Instance.get().typeTimeCaption();
    }

    @Override
    public GEditBindingMap.EditEventFilter getEditEventFilter() {
        return GEditBindingMap.numberEventFilter;
    }
}
