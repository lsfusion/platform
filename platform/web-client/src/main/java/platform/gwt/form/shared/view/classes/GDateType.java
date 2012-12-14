package platform.gwt.form.shared.view.classes;

import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.DateGridCellEditor;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.renderer.DateGridCellRenderer;
import platform.gwt.form.shared.view.grid.renderer.GridCellRenderer;

public class GDateType extends GDataType {
    public static GDateType instance = new GDateType();

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new DateGridCellEditor(editManager);
    }

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new DateGridCellRenderer();
    }

    @Override
    public String getPreferredMask() {
        return "01.01.2001";
    }
}
