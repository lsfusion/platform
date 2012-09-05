package platform.gwt.form2.shared.view.classes;

import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.grid.EditManager;
import platform.gwt.form2.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form2.shared.view.grid.editor.IntegerGridEditor;

public class GLongType extends GIntegralType {
    public static GLongType instance = new GLongType();

    @Override
    public Object parseString(String strValue) {
        return Long.parseLong(strValue);
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new IntegerGridEditor(editManager);
    }
}
