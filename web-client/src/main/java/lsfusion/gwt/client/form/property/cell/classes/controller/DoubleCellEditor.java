package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.i18n.client.NumberFormat;
import lsfusion.gwt.client.classes.data.GDoubleType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import java.math.BigDecimal;

public class DoubleCellEditor extends IntegralCellEditor {
    public DoubleCellEditor(EditManager editManager, GPropertyDraw property, NumberFormat format) {
        super(GDoubleType.instance, editManager, property, format);
    }

    @Override
    protected String renderToString(Object value) {
        if (value != null) {
            assert value instanceof Number;
            return format.format(new BigDecimal(value.toString()));
        }
        return "";
    }
}