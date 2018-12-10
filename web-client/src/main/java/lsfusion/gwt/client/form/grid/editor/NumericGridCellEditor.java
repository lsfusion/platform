package lsfusion.gwt.form.client.grid.editor;

import com.google.gwt.i18n.client.NumberFormat;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.classes.GNumericType;
import lsfusion.gwt.form.client.grid.EditManager;

public class NumericGridCellEditor extends IntegralGridCellEditor {
    public NumericGridCellEditor(GNumericType numericType, EditManager editManager, GPropertyDraw property, NumberFormat format) {
        super(numericType, editManager, property, format);
    }
}
