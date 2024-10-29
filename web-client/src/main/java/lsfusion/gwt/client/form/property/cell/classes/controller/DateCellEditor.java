package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JsDate;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.classes.data.GADateType;
import lsfusion.gwt.client.classes.data.GFormatType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import java.text.ParseException;

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
    protected String getSinglePattern() {
        return replaceUnsupportedSymbols(type.getFormat(pattern).getPattern());
    }

    protected boolean isTimeEditor() {
        return false;
    }

    protected boolean isDateEditor() {
        return true;
    }

    @Override
    protected boolean isSinglePicker() {
        return true;
    }

//    @Override
//    public PValue getDefaultNullValue() {
//        return type.fromDate(new Date());
//    }

    @Override
    protected String tryFormatInputText(PValue value) {
        if(isNative()) {
            if (value == null)
                return "";

            return type.formatISOString(value);
        }

        return super.tryFormatInputText(value);
    }

    @Override
    protected PValue tryParseInputText(String inputText, boolean onCommit) throws ParseException {
        if(isNative()) {
            if (inputText.isEmpty())
                return null;

            return type.parseISOString(inputText);
        }

        return super.tryParseInputText(inputText, onCommit);
    }

    @Override
    protected JsDate getStartDate(PValue oldValue) {
        return type.toJsDate(oldValue);
    }

    @Override
    protected JsDate getEndDate(PValue oldValue) {
        return type.toJsDate(oldValue);
    }

    @Override
    protected PValue getValue(JsDate startDate, JsDate endDate) {
        return startDate != null ? type.fromJsDate(startDate) : null;
    }
}
