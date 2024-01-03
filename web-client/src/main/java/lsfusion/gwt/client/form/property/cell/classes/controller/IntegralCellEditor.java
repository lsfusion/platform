package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.i18n.client.LocaleInfo;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.classes.data.GFormatType;
import lsfusion.gwt.client.classes.data.GIntegralType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
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
    protected PValue tryParseInputText(String inputText, boolean onCommit) throws ParseException {
        if(isNative()) {
            if (inputText.isEmpty())
                return null;

            return type.parseISOString(inputText);
        }

        if (inputText.isEmpty() || (onCommit && "-".equals(inputText)))
            return null;

        inputText = inputText.replace(" ", "").replace(GIntegralType.UNBREAKABLE_SPACE, "");
        if (!onCommit && "-".equals(inputText))
            return PValue.getPValue(0);

        return super.tryParseInputText(inputText, onCommit);
    }

    @Override
    protected String tryFormatInputText(PValue value) {
        if(isNative()) {
            if (value == null)
                return "";

            return type.formatISOString(value);
        }

        String result = super.tryFormatInputText(value);

        String groupingSeparator = LocaleInfo.getCurrentLocale().getNumberConstants().groupingSeparator();
        result = result.replace(groupingSeparator, "");

        return result;
    }
}