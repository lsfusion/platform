package lsfusion.gwt.shared.view.classes;

import com.google.gwt.i18n.shared.DateTimeFormat;
import lsfusion.gwt.shared.GwtSharedUtils;
import lsfusion.gwt.client.form.MainFrameMessages;
import lsfusion.gwt.shared.view.GEditBindingMap;
import lsfusion.gwt.shared.view.GPropertyDraw;
import lsfusion.gwt.client.form.grid.EditManager;
import lsfusion.gwt.client.form.grid.editor.AbstractGridCellEditor;
import lsfusion.gwt.client.form.grid.editor.GridCellEditor;
import lsfusion.gwt.client.form.grid.renderer.DateGridCellRenderer;
import lsfusion.gwt.client.form.grid.renderer.GridCellRenderer;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Date;

import static lsfusion.gwt.shared.GwtSharedUtils.*;

public class GDateTimeType extends GFormatType<com.google.gwt.i18n.client.DateTimeFormat> {
    public static GDateTimeType instance = new GDateTimeType();

    @Override
    public com.google.gwt.i18n.client.DateTimeFormat getFormat(String pattern) {
        return GwtSharedUtils.getDateTimeFormat(pattern);
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
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return AbstractGridCellEditor.createGridCellEditor(this, editManager, editProperty);
    }

    @Override
    public Timestamp parseString(String value, String pattern) throws ParseException {
        if (value.isEmpty()) {
            return null;
        }
        Date date = GDateType.parseDate(value, getDefaultDateTimeFormat(), getDefaultDateTimeShortFormat(), getDefaultDateFormat());
        return new Timestamp(date.getTime());
    }

    private static Date wideFormattableDateTime = null;

    public static Date getWideFormattableDateTime() {
        if(wideFormattableDateTime == null)
            wideFormattableDateTime = DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss").parse("1991-11-21 10:55:55");
        return wideFormattableDateTime;
    }

    @Override
    protected Object getDefaultWidthValue() {
        return getWideFormattableDateTime();
    }

    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeDateTimeCaption();
    }

    @Override
    public GEditBindingMap.EditEventFilter getEditEventFilter() {
        return GEditBindingMap.numberEventFilter;
    }
}
