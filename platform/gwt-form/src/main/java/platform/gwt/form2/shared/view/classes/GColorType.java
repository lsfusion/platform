package platform.gwt.form2.shared.view.classes;

import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.grid.EditManager;
import platform.gwt.form2.shared.view.grid.editor.ColorGridEditor;
import platform.gwt.form2.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form2.shared.view.grid.renderer.ColorGridRenderer;
import platform.gwt.form2.shared.view.grid.renderer.GridCellRenderer;

public class GColorType extends GDataType {
    public static GColorType instance = new GColorType();

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new ColorGridEditor(editManager);
    }

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new ColorGridRenderer();
    }

    @Override
    public String getPreferredMask() {
        return "";
    }

    @Override
    public String getMinimumWidth(int minimumCharWidth) {
        return "40px";
    }
}
