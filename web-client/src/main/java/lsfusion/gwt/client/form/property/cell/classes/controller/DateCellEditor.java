package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JsDate;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.classes.data.GADateType;
import lsfusion.gwt.client.classes.data.GFormatType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import java.text.ParseException;
import java.util.Date;

public class DateCellEditor extends DateRangePickerBasedCellEditor {

    protected GADateType type;

    public DateCellEditor(GADateType type, EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
        this.type = type;
    }

    @Override
    public GFormatType getFormatType() {
        return type;
    }

    @Override
    protected JsDate getStartDate(PValue oldValue) {
        return GwtClientUtils.toJsDate(type.toDate(oldValue));
    }

    @Override
    protected JsDate getEndDate(PValue oldValue) {
        return GwtClientUtils.toJsDate(type.toDate(oldValue));
    }

    @Override
    protected String getPattern() {
        return type.getFormat(property.pattern).getPattern();
    }

    protected boolean isTimeEditor() {
        return false;
    }

    protected boolean isDateEditor() {
        return true;
    }

    @Override
    protected PValue getDateInputValue() {
        JsDate pickerStartDate = getPickerStartDate();
        return pickerStartDate != null ? type.fromDate(GwtClientUtils.fromJsDate(pickerStartDate)) : null;
    }

    @Override
    protected boolean isSinglePicker() {
        return true;
    }

    @Override
    public PValue getDefaultNullValue() {
        return type.fromDate(new Date());
    }

    // valueAsDate doesn't work for datetime + hours
//    public native static JsDate getDateInputValue(InputElement element)/*-{
//        return element.valueAsDate;
//    }-*/;
//
//    @Override
//    public PValue getCommitValue(Element parent, Integer contextAction) throws InvalidEditException {
//        if(popup == null) // if we have nativePopup (ie date, datetime-local, time)
//            return type.fromDate(GwtClientUtils.fromJsDate(getDateInputValue(inputElement)));
//        return super.getCommitValue(parent, contextAction);
//    }

    @Override
    protected String tryFormatInputText(PValue value) {
        if(popup == null) {
            if (value == null)
                return "";

            return type.formatISOString(value);
        }

        return super.tryFormatInputText(value);
    }

    @Override
    protected PValue tryParseInputText(String inputText, boolean onCommit) throws ParseException {
        if(popup == null) {
            if (inputText == null || inputText.isEmpty())
                return null;

            return type.parseISOString(inputText);
        }

        return super.tryParseInputText(inputText, onCommit);
    }
}
