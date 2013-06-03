package lsfusion.gwt.form.shared.view.classes;

import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.grid.EditManager;
import lsfusion.gwt.form.shared.view.grid.editor.GridCellEditor;
import lsfusion.gwt.form.shared.view.grid.editor.IntegerGridCellEditor;

import java.text.ParseException;

public class GIntegerType extends GIntegralType {
    public static GIntegerType instance = new GIntegerType();

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new IntegerGridCellEditor(editManager, editProperty);
    }

    @Override
    public Object parseString(String s) throws ParseException {
        try {
            return s.isEmpty() ? null : Integer.parseInt(s.replaceAll(" ", ""));
        } catch (NumberFormatException e) {
            throw new ParseException("string " + s + "can not be converted to integer", 0);
        }
    }

    @Override
    public String toString() {
        return "Целое число";
    }
}
