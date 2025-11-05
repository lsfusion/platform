package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.i18n.client.LocaleInfo;
import lsfusion.gwt.client.classes.data.GFormatType;
import lsfusion.gwt.client.classes.data.GIntegralType;
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

    @Override
    protected void setInputValue(String value) {
        // input type="number" in IOS crashes if value is an empty string or zero
        if (value == null || value.isEmpty())
            return;

        // input type number does not support does not support commas, only periods are allowed.
        if (property.inputType.isNumber())
            inputElement.setValue(value.replace(",", "."));
        else
            super.setInputValue(value);
    }

}