package platform.gwt.form.shared.view.classes;

import com.google.gwt.i18n.client.NumberFormat;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.DoubleGridCellEditor;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;

import java.text.ParseException;

public class GDoubleType extends GIntegralType {
    public static GDoubleType instance = new GDoubleType();

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new DoubleGridCellEditor(editManager, editProperty);
    }

    @Override
    public Object parseString(String s) throws ParseException {
        try {
            return s.isEmpty() ? null : NumberFormat.getDecimalFormat().parse(s.replaceAll(" ", ""));
        } catch (NumberFormatException e) {
            throw new ParseException("string " + s + "can not be converted to double", 0);
        }
    }

    @Override
    public String toString() {
        return "Вещественное число";
    }
}
