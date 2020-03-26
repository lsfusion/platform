package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.i18n.client.NumberFormat;
import lsfusion.gwt.client.classes.data.GIntegralType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import java.text.ParseException;

public class IntegralGridCellEditor extends TextBasedGridCellEditor {
    protected final NumberFormat format;

    protected final GIntegralType type;

    public IntegralGridCellEditor(GIntegralType type, EditManager editManager, GPropertyDraw property) {
        this(type, editManager, property, NumberFormat.getDecimalFormat());
    }

    public IntegralGridCellEditor(GIntegralType type, EditManager editManager, GPropertyDraw property, NumberFormat format) {
        super(editManager, property);
        this.format = format;
        this.type = type;
    }

    @Override
    protected String renderToString(Object value) {
        if (value != null) {
            assert value instanceof Number;
            return format.format((Number) value);
        }
        return "";
    }

    @Override
    protected Object tryParseInputText(String inputText, boolean onCommit) throws ParseException {
        if (inputText.isEmpty() || (onCommit && "-".equals(inputText))) {
            return null;
        } else {
            inputText = inputText.replace(" ", "").replace(GIntegralType.UNBREAKABLE_SPACE, "");
            return (!onCommit && "-".equals(inputText)) ? true : type.parseString(inputText, property.pattern);
        }
    }
}