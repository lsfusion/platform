package platform.gwt.form.shared.view.classes;

import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.DoubleGridCellEditor;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;

public class GDoubleType extends GIntegralType {
    public static GDoubleType instance = new GDoubleType();

    @Override
    public Object parseString(String strValue) {
        return Double.parseDouble(strValue);
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new DoubleGridCellEditor(editManager, editProperty);
    }
}
