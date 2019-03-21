package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.i18n.client.NumberFormat;
import lsfusion.gwt.shared.form.property.GPropertyDraw;
import lsfusion.gwt.shared.classes.data.GNumericType;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

public class NumericGridCellEditor extends IntegralGridCellEditor {
    public NumericGridCellEditor(GNumericType numericType, EditManager editManager, GPropertyDraw property, NumberFormat format) {
        super(numericType, editManager, property, format);
    }
}
