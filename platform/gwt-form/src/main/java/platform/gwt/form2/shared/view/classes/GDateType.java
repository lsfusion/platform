package platform.gwt.form2.shared.view.classes;

import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.grid.EditManager;
import platform.gwt.form2.shared.view.grid.editor.DateGridEditor;
import platform.gwt.form2.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form2.shared.view.grid.renderer.DateGridRenderer;
import platform.gwt.form2.shared.view.grid.renderer.GridCellRenderer;

public class GDateType extends GDataType {
    public static GDateType instance = new GDateType();

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new DateGridEditor(editManager);
    }

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new DateGridRenderer();
    }

    @Override
    public String getPreferredMask() {
        return "01.01.2001";
    }
}
