package lsfusion.gwt.form.shared.view.grid.editor;

import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.grid.EditManager;

import java.math.BigDecimal;

public class NumericGridCellEditor extends DoubleGridCellEditor {
    public NumericGridCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
    }

    @Override
    protected Object parseNotNullString(String numericString) {
        return BigDecimal.valueOf(format.parse(numericString));
    }

    @Override
    protected boolean isStringValid(String string) {
        try {
            BigDecimal.valueOf(format.parse(string));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
