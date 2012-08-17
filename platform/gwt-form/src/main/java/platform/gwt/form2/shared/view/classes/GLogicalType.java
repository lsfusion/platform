package platform.gwt.form2.shared.view.classes;

import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.grid.EditManager;
import platform.gwt.form2.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form2.shared.view.grid.editor.LogicalGridEditor;
import platform.gwt.form2.shared.view.grid.renderer.GridCellRenderer;
import platform.gwt.form2.shared.view.grid.renderer.LogicalGridRenderer;

public class GLogicalType extends GDataType {
    public static GLogicalType instance = new GLogicalType();

//    @Override
//    public PanelRenderer createPanelRenderer(GFormController form, GPropertyDraw property) {
//        return new LogicalPanelRenderer(property);
//    }

    @Override
    public GridCellRenderer createGridCellRenderer() {
        return new LogicalGridRenderer();
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty, Object oldValue) {
        return new LogicalGridEditor(editManager, oldValue);
    }
}
