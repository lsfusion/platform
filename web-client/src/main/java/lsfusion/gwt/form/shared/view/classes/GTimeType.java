package lsfusion.gwt.form.shared.view.classes;

import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.gwt.form.client.MainFrameMessages;
import lsfusion.gwt.form.shared.view.GEditBindingMap;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.changes.dto.GTimeDTO;
import lsfusion.gwt.form.shared.view.grid.EditManager;
import lsfusion.gwt.form.shared.view.grid.editor.AbstractGridCellEditor;
import lsfusion.gwt.form.shared.view.grid.editor.GridCellEditor;
import lsfusion.gwt.form.shared.view.grid.renderer.DateGridCellRenderer;
import lsfusion.gwt.form.shared.view.grid.renderer.GridCellRenderer;

import java.text.ParseException;

import static lsfusion.gwt.base.shared.GwtSharedUtils.getDefaultTimeFormat;
import static lsfusion.gwt.base.shared.GwtSharedUtils.getDefaultTimeShortFormat;
import static lsfusion.gwt.form.shared.view.classes.GDateType.parseDate;

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
    public GridCellEditor visit(GTypeVisitor visitor) {
        return (GridCellEditor) visitor.visit(this);
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return AbstractGridCellEditor.createGridCellEditor(this, editManager, editProperty);
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
