package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.classes.data.GFormatType;
import lsfusion.gwt.client.classes.data.GIntegralType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.view.MainFrame;

import java.text.ParseException;

public class IntegralCellEditor extends TextBasedCellEditor implements FormatCellEditor {
    protected final GIntegralType type;

    public IntegralCellEditor(GIntegralType type, EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
        this.type = type;
    }

    @Override
    public GFormatType getFormatType() {
        return type;
    }

    @Override
    public Element createInputElement(Element parent) {
        Element element = super.createInputElement(parent);
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
            return (!onCommit && "-".equals(inputText)) ? true : super.tryParseInputText(inputText, onCommit);
        }
    }

    @Override
    protected String tryFormatInputText(Object value) {
        String result = super.tryFormatInputText(value);
        if(result != null)
            result = GwtClientUtils.editFormat(result);
        return result;
    }
}