package platform.gwt.form.shared.view.classes;

import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.editor.StringGridCellEditor;

public class GVarStringType extends GStringType {
    public GVarStringType() {}

    public GVarStringType(int length) {
        this(length, false);
    }

    public GVarStringType(int length, boolean caseInsensitive) {
        super(length, caseInsensitive);
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new StringGridCellEditor(editManager, editProperty);
    }

    @Override
    public String toString() {
        return "Строка без паддинга " + (caseInsensitive ? " без регистра" : "") + "(" + length + ")";
    }
}
