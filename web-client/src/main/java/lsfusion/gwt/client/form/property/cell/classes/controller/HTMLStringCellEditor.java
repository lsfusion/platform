package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.base.GAsync;
import lsfusion.gwt.client.base.view.popup.PopupMenuItemValue;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.classes.controller.suggest.GCompletionType;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import java.text.ParseException;

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
        if(string == null || EscapeUtils.isContainHtmlTag(string) || string.contains("\n"))
            string = "";
        return string;
    }

//    here it doesn't really matter if we use ULTRA_STRICT
//    @Override
//    protected PValue tryParseInputText(String inputText, boolean onCommit) throws ParseException {
//    }
}
