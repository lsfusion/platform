package lsfusion.gwt.form.shared.view.classes;

import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.gwt.form.shared.view.GEditBindingMap;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.grid.EditManager;
import lsfusion.gwt.form.shared.view.grid.editor.DateGridCellEditor;
import lsfusion.gwt.form.shared.view.grid.editor.GridCellEditor;
import lsfusion.gwt.form.shared.view.grid.renderer.DateGridCellRenderer;
import lsfusion.gwt.form.shared.view.grid.renderer.GridCellRenderer;

import java.text.ParseException;
import java.util.Date;

public class GDateType extends GDataType {
    public static GDateType instance = new GDateType();

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new DateGridCellEditor(editManager, editProperty);
    }

    @Override
    public java.sql.Date parseString(String s) throws ParseException {
        DateTimeFormat format = GwtSharedUtils.getDefaultDateFormat();
        if (s.isEmpty()) {
            return null;
        } else {
            Date resultDate;
            try {
                if (s.split("\\.").length == 2) {
                    resultDate = format.parse(s + "." + (new Date().getYear() - 100));
                } else {
                    resultDate = format.parse(s);
                }
            } catch (IllegalArgumentException e) {
                throw new ParseException("string " + s + "can not be converted to date", 0);
            }
            return GwtSharedUtils.safeDateToSql(resultDate);
        }
    }

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new DateGridCellRenderer(property);
    }

    @Override
    public String getPreferredMask() {
        return "01.01.01";
    }

    @Override
    public String toString() {
        return "Дата";
    }

    @Override
    public GEditBindingMap.EditEventFilter getEditEventFilter() {
        return GEditBindingMap.numberEventFilter;
    }
}
