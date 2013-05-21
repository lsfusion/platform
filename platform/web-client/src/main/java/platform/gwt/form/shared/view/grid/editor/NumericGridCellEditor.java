package platform.gwt.form.shared.view.grid.editor;

import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditManager;

import java.math.BigDecimal;

public class NumericGridCellEditor extends DoubleGridCellEditor {
    public NumericGridCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
    }

    @Override
    protected Object parseNotNullString(String numericString) {
        return BigDecimal.valueOf(format.parse(numericString));
    }
}
