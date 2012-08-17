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

//    @Override
//    public PanelRenderer createPanelRenderer(GFormController form, GPropertyDraw property) {
//        return new LongPanelRenderer(property);
//    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty, Object oldValue) {
        return new IntegerGridEditor(editManager, oldValue);
    }
}
