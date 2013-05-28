package platform.gwt.form.shared.view.classes;

import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.editor.StringGridCellEditor;
import platform.gwt.form.shared.view.grid.renderer.GridCellRenderer;
import platform.gwt.form.shared.view.grid.renderer.StringGridCellRenderer;

public class GVarStringType extends GStringType {
    public GVarStringType() {}

    public GVarStringType(int length) {
        this(length, false);
    }

    public GVarStringType(int length, boolean caseInsensitive) {
        super(length, caseInsensitive);
    }

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new StringGridCellRenderer(property, true);
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new StringGridCellEditor(editManager, editProperty, true);
    }

    @Override
    public String toString() {
        return "Строка без паддинга " + (caseInsensitive ? " без регистра" : "") + "(" + length + ")";
    }
}
