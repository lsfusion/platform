package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.i18n.client.constants.NumberConstants;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.classes.data.GDoubleType;
import lsfusion.gwt.client.classes.data.GFormatType;
import lsfusion.gwt.client.classes.data.GIntegralType;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.IntegralPatternConverter;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

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

    protected boolean isNative() {
        return inputElementType.isNumber();
    }

    @Override
    protected JavaScriptObject getMaskFromPattern() {
        return IntegralPatternConverter.convert(pattern);
    }

    @Override
    public void onBrowserEvent(Element parent, EventHandler handler) {
        Event event = handler.event;
        // there are no grouping separators in the editor (see tryFormatInputText), so a typed one is meant as the decimal separator ("," on the numpad in the ru layout, when the locale is en)
        // masked (inputmask substituteRadixPoint) and native inputs treat it that way, but here the parser just skips it: 10,22 -> 1022
        // integer types have no decimal separator to input, so for them nothing changes
        if (!isNative() && mask == null && type instanceof GDoubleType && GKeyStroke.isCharAddKeyEvent(event)) {
            NumberConstants constants = LocaleInfo.getCurrentLocale().getNumberConstants();
            String typed = String.valueOf((char) event.getCharCode());
            if ((typed.equals(",") || typed.equals(".")) && typed.equals(constants.groupingSeparator())) {
                insertText(event, constants.decimalSeparator());
                handler.consume();
                return;
            }
        }

        super.onBrowserEvent(parent, handler);
    }

    @Override
    protected PValue tryParseInputText(String inputText, boolean onCommit) throws ParseException {
        if(isNative()) {
            if (inputText.isEmpty())
                return null;

            return type.parseISOString(inputText);
        }

        if (inputText.isEmpty() || (onCommit && "-".equals(inputText)))
            return null;
        for(String replace : new String[] {" ", GIntegralType.UNBREAKABLE_SPACE, "\r", "\n"}) {
            inputText = inputText.replace(replace, "");
        }
        if (!onCommit && "-".equals(inputText))
            return PValue.getPValue(0);

        return super.tryParseInputText(inputText, onCommit);
    }

    @Override
    protected String tryFormatInputText(PValue value) {
        if(isNative())
            return type.formatISOString(value);

        String result = super.tryFormatInputText(value);

        String groupingSeparator = LocaleInfo.getCurrentLocale().getNumberConstants().groupingSeparator();
        result = result.replace(groupingSeparator, "");

        return result;
    }
}