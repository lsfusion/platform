package lsfusion.gwt.form.shared.view.classes;

import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.gwt.form.shared.view.GEditBindingMap;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.changes.dto.GTimeDTO;
import lsfusion.gwt.form.shared.view.grid.EditManager;
import lsfusion.gwt.form.shared.view.grid.editor.GridCellEditor;
import lsfusion.gwt.form.shared.view.grid.editor.TimeGridCellEditor;
import lsfusion.gwt.form.shared.view.grid.renderer.DateGridCellRenderer;
import lsfusion.gwt.form.shared.view.grid.renderer.GridCellRenderer;

import java.text.ParseException;

import static lsfusion.gwt.base.shared.GwtSharedUtils.getDefaultTimeFormat;
import static lsfusion.gwt.base.shared.GwtSharedUtils.getDefaultTimeShortFormat;
import static lsfusion.gwt.form.shared.view.classes.GDateType.parseDate;

public class GTimeType extends GDataType {
    public static GTimeType instance = new GTimeType();

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new DateGridCellRenderer(property, GwtSharedUtils.getDefaultTimeFormat());
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
    public String getPreferredMask(String pattern) {
        return pattern != null ? pattern : "00:00:00";
    }

    @Override
    public String toString() {
        return "Время";
    }

    @Override
    public GEditBindingMap.EditEventFilter getEditEventFilter() {
        return GEditBindingMap.numberEventFilter;
    }
}
