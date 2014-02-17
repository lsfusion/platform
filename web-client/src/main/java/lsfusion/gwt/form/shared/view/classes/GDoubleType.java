package lsfusion.gwt.form.shared.view.classes;

import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.grid.EditManager;
import lsfusion.gwt.form.shared.view.grid.editor.DoubleGridCellEditor;
import lsfusion.gwt.form.shared.view.grid.editor.GridCellEditor;

import java.text.ParseException;

public class GDoubleType extends GIntegralType {
    public static GDoubleType instance = new GDoubleType();

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new DoubleGridCellEditor(editManager, editProperty);
    }

    @Override
    public Object parseString(String s) throws ParseException {
        return parseToDouble(s);
    }

    @Override
    public String toString() {
        return "Вещественное число";
    }
}
