package platform.gwt.view2.classes;

import platform.gwt.view2.GPropertyDraw;
import platform.gwt.view2.grid.EditManager;
import platform.gwt.view2.grid.editor.GridCellEditor;
import platform.gwt.view2.grid.editor.IntegerGridEditor;
import platform.gwt.view2.logics.FormLogicsProvider;
import platform.gwt.view2.panel.IntegerPanelRenderer;
import platform.gwt.view2.panel.PanelRenderer;

public class GIntegerType extends GIntegralType {
    public static GType instance = new GIntegerType();

    @Override
    public Object parseString(String strValue) {
        return Integer.parseInt(strValue);
    }

    @Override
    public PanelRenderer createPanelRenderer(FormLogicsProvider formLogics, GPropertyDraw property) {
        return new IntegerPanelRenderer(property);
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty, Object oldValue) {
        return new IntegerGridEditor(editManager, oldValue);
    }
}
