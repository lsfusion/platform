package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JsDate;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.classes.data.GADateType;
import lsfusion.gwt.client.classes.data.GFormatType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

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
    protected JsDate getStartDate(Object oldValue) {
        return GwtClientUtils.toJsDate(type.toDate(oldValue));
    }

    @Override
    protected JsDate getEndDate(Object oldValue) {
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
    protected Object getInputValue() {
        JsDate pickerStartDate = getPickerStartDate(false);
        return pickerStartDate != null ? type.fromDate(GwtClientUtils.fromJsDate(pickerStartDate)) : null;
    }

    @Override
    protected boolean isSinglePicker() {
        return true;
    }

    @Override
    public Object getDefaultNullValue() {
        return type.fromDate(new Date());
    }
}
