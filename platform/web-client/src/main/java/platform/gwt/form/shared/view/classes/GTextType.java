package platform.gwt.form.shared.view.classes;

import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.editor.TextGridCellEditor;
import platform.gwt.form.shared.view.grid.renderer.GridCellRenderer;
import platform.gwt.form.shared.view.grid.renderer.TextGridCellRenderer;

public class GTextType extends GDataType {
    public static GTextType instance = new GTextType();

    @Override
    public String getMinimumMask() {
        return "999 999";
    }

    @Override
    public String getPreferredMask() {
        return "9 999 999";
    }

    @Override
    public int getMinimumPixelHeight() {
        return super.getMinimumPixelHeight() * 4;
    }

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new TextGridCellRenderer();
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new TextGridCellEditor(editManager);
    }
}
