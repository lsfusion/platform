package lsfusion.gwt.client.form.ui.grid.editor;

import com.google.gwt.i18n.client.NumberFormat;
import lsfusion.gwt.shared.view.GPropertyDraw;
import lsfusion.gwt.shared.view.classes.GNumericType;
import lsfusion.gwt.client.form.ui.grid.EditManager;

public class NumericGridCellEditor extends IntegralGridCellEditor {
    public NumericGridCellEditor(GNumericType numericType, EditManager editManager, GPropertyDraw property, NumberFormat format) {
        super(numericType, editManager, property, format);
    }
}
