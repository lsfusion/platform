package lsfusion.gwt.form.shared.view.classes;

import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.gwt.form.client.MainFrameMessages;
import lsfusion.gwt.form.shared.view.GEditBindingMap;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.changes.dto.GDateDTO;
import lsfusion.gwt.form.shared.view.grid.EditManager;
import lsfusion.gwt.form.shared.view.grid.editor.AbstractGridCellEditor;
import lsfusion.gwt.form.shared.view.grid.editor.GridCellEditor;
import lsfusion.gwt.form.shared.view.grid.renderer.DateGridCellRenderer;
import lsfusion.gwt.form.shared.view.grid.renderer.GridCellRenderer;

import java.text.ParseException;
import java.util.Date;

import static lsfusion.gwt.base.shared.GwtSharedUtils.getDateFormat;

public class GDateType extends GFormatType<DateTimeFormat> {

    public static GDateType instance = new GDateType();

    @Override
    public DateTimeFormat getFormat(String pattern) {
        return GwtSharedUtils.getDateFormat(pattern);
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return AbstractGridCellEditor.createGridCellEditor(this, editManager, editProperty);
    }

    @Override
    public GDateDTO parseString(String value, String pattern) throws ParseException {
        return value.isEmpty() ? null : GDateDTO.fromDate(parseDate(value, getDateFormat(pattern)));
    }

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new DateGridCellRenderer(property);
    }

    @Override
    public GridCellEditor visit(GTypeVisitor visitor) {
        return (GridCellEditor) visitor.visit(this);
    }

    @Override
    protected Object getDefaultWidthValue() {
        return GDateTimeType.getWideFormattableDateTime();
    }

    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeDateCaption();
    }

    @Override
    public GEditBindingMap.EditEventFilter getEditEventFilter() {
        return GEditBindingMap.numberEventFilter;
    }

    public static Date parseDate(String value, DateTimeFormat... formats) throws ParseException {
        for (DateTimeFormat format : formats) {
            try {
                return format.parse(value);
            } catch (IllegalArgumentException ignore) {
            }
        }
        throw new ParseException("string " + value + "can not be converted to date", 0);
    }
}
