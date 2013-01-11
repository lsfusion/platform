package platform.gwt.form.shared.view.classes;

import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.editor.IntegerGridCellEditor;

public class GIntegerType extends GIntegralType {
    public static GIntegerType instance = new GIntegerType();

    @Override
    public Object parseString(String strValue) {
        return Integer.parseInt(strValue);
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new IntegerGridCellEditor(editManager, editProperty);
    }
}
