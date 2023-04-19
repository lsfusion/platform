package lsfusion.gwt.client.form.property.cell.classes.controller;

import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.classes.data.GFormatType;
import lsfusion.gwt.client.classes.data.GIntegralType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import java.text.ParseException;

public class IntegralCellEditor extends SimpleTextBasedCellEditor implements FormatCellEditor {
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
    protected PValue tryParseInputText(String inputText, boolean onCommit) throws ParseException {
        if (inputText.isEmpty() || (onCommit && "-".equals(inputText))) {
            return null;
        } else {
            inputText = inputText.replace(" ", "").replace(GIntegralType.UNBREAKABLE_SPACE, "");
            return (!onCommit && "-".equals(inputText)) ? PValue.getPValue(0) : super.tryParseInputText(inputText, onCommit);
        }
    }

    @Override
    protected String tryFormatInputText(PValue value) {
        String result = super.tryFormatInputText(value);
        if(result != null)
            result = GwtClientUtils.editFormat(result);
        return result;
    }
}