package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JsDate;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.classes.data.GFormatType;
import lsfusion.gwt.client.classes.data.GIntervalType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

public class IntervalCellEditor  extends DateRangePickerBasedCellEditor {

    private final String intervalType;
    private final GIntervalType type;

    public IntervalCellEditor(EditManager editManager, GPropertyDraw property, String intervalType, GIntervalType type) {
        super(editManager, property);
        this.intervalType = intervalType;
        this.type = type;
    }

    @Override
    public GFormatType getFormatType() {
        return type;
    }

//    @Override
//    protected JsDate getStartDate(PValue oldValue) {
//        return GwtClientUtils.toJsDate(type.toDate(oldValue, true));
//    }
//
//    @Override
//    protected JsDate getEndDate(PValue oldValue) {
//        return GwtClientUtils.toJsDate(type.toDate(oldValue, false));
//    }

    @Override
    protected String getSinglePattern() {
        return type.getSingleFormat(pattern).getPattern();
    }

    @Override
    protected boolean isTimeEditor() {
        return intervalType.equals("TIME");
    }

    @Override
    protected boolean isDateEditor() {
        return intervalType.equals("DATE");
    }

//    @Override
//    protected PValue getDateInputValue() {
//        return type.fromDate(GwtClientUtils.fromJsDate(getPickerStartDate()), GwtClientUtils.fromJsDate(getPickerEndDate()));
//    }

    @Override
    protected boolean isSinglePicker() {
        return false;
    }

//    @Override
//    public PValue getDefaultNullValue() {
//        return type.fromDate(new Date(), new Date());
//    }
}
