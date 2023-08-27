package lsfusion.gwt.client.form.property.cell.classes.controller;

import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.classes.controller.suggest.GCompletionType;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

public class HTMLStringCellEditor extends SimpleTextBasedCellEditor {

    public HTMLStringCellEditor(EditManager editManager, GPropertyDraw property, GInputList inputList) {
        super(editManager, property, inputList);

        if(compare == GCompare.MATCH || compare == GCompare.CONTAINS)
            completionType = GCompletionType.SEMI_ULTRA_NON_STRICT;
        else
            completionType = GCompletionType.ULTRA_STRICT; // we're changing the completion to ULTRA_STRICT, to avoid problems with input and /r
    }

    @Override
    protected String tryFormatInputText(PValue value) {
        String string = PValue.getStringValue(value);
        if(string == null || GwtClientUtils.isContainHtmlTag(string) || string.contains("\n"))
            string = "";
        return string;
    }

//    here it doesn't really matter if we use ULTRA_STRICT
//    @Override
//    protected PValue tryParseInputText(String inputText, boolean onCommit) throws ParseException {
//    }
}
