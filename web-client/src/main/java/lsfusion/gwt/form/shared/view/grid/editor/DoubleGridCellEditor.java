package lsfusion.gwt.form.shared.view.grid.editor;

import com.google.gwt.i18n.client.NumberFormat;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.classes.GDoubleType;
import lsfusion.gwt.form.shared.view.grid.EditManager;

import java.math.BigDecimal;

public class DoubleGridCellEditor extends IntegralGridCellEditor {
    public DoubleGridCellEditor(EditManager editManager, GPropertyDraw property, NumberFormat format) {
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