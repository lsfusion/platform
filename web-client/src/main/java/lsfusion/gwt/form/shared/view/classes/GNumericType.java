package lsfusion.gwt.form.shared.view.classes;

import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.grid.EditManager;
import lsfusion.gwt.form.shared.view.grid.editor.GridCellEditor;
import lsfusion.gwt.form.shared.view.grid.editor.NumericGridCellEditor;

public class GNumericType extends GDoubleType {
    private int length = 10;
    private int precision = 2;

    public GNumericType() {}

    public GNumericType(int length, int precision) {
        this.length = length;
        this.precision = precision;
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new NumericGridCellEditor(editManager, editProperty);
    }

    @Override
    public String toString() {
        return "Число" + '[' + length + ',' + precision + ']';
    }
}
