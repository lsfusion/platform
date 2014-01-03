package lsfusion.gwt.form.shared.view.grid.editor;

import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.classes.GNumericType;
import lsfusion.gwt.form.shared.view.grid.EditManager;

public class NumericGridCellEditor extends IntegralGridCellEditor {
    public NumericGridCellEditor(GNumericType numericType, EditManager editManager, GPropertyDraw property) {
        super(numericType, editManager, property);
    }
}
