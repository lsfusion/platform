package platform.gwt.form.shared.view.classes;

import platform.gwt.form.shared.view.GFont;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.editor.TextGridCellEditor;
import platform.gwt.form.shared.view.grid.renderer.GridCellRenderer;
import platform.gwt.form.shared.view.grid.renderer.TextGridCellRenderer;

public class GTextType extends GAbstractStringType {
    public static GTextType instance = new GTextType();

    public GTextType() {
        super(false);
    }

    @Override
    public String getMinimumMask() {
        return "999 999";
    }

    @Override
    public String getPreferredMask() {
        return "9 999 999";
    }

    @Override
    public int getMinimumPixelHeight(GFont font) {
        return super.getMinimumPixelHeight(font) * 4;
    }

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new TextGridCellRenderer(property);
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new TextGridCellEditor(editManager, editProperty);
    }

    @Override
    public String toString() {
        return "Текст";
    }
}
