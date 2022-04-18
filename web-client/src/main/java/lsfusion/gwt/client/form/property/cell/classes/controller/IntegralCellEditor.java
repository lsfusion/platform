package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.NumberFormat;
import lsfusion.gwt.client.classes.data.GIntegralType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.view.MainFrame;

import java.text.ParseException;

public class IntegralCellEditor extends TextBasedCellEditor {
    protected final NumberFormat format;

    protected final GIntegralType type;

    public IntegralCellEditor(GIntegralType type, EditManager editManager, GPropertyDraw property) {
        this(type, editManager, property, NumberFormat.getDecimalFormat());
    }

    public IntegralCellEditor(GIntegralType type, EditManager editManager, GPropertyDraw property, NumberFormat format) {
        super(editManager, property);
        this.format = format;
        this.type = type;
    }

    @Override
    public Element createInputElement() {
        Element element = super.createInputElement();
        if(MainFrame.mobile) {
            element.setAttribute("type", "number");
            element.setAttribute("step", "0.01");
        }
        return element;
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

    @Override
    protected String tryFormatInputText(Object value) {
        if (value instanceof String) {
            try {
                value = tryParseInputText((String) value, false);
            } catch (ParseException e) {
                return "";
            }
        }

        if (value instanceof Number)
            return type.formatDouble(((Number) value).doubleValue(), property.pattern);

        return "";
    }
}