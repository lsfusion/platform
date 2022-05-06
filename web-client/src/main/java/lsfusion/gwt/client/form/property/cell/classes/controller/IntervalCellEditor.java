package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JsDate;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.SimplePanel;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.classes.data.GFormatType;
import lsfusion.gwt.client.classes.data.GIntervalType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import java.util.Date;

public class IntervalCellEditor extends TextBasedPopupCellEditor implements FormatCellEditor  {

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

    @Override
    public void stop(Element parent, boolean cancel, boolean blurred) {
        removePicker();

        super.stop(parent, cancel, blurred);
    }

    public SimplePanel createPopupComponent(Element parent, Object oldValue) {
        assert oldValue != null;
        createPicker(parent, GwtClientUtils.toJsDate(type.toDate(oldValue, true)),
                GwtClientUtils.toJsDate(type.toDate(oldValue, false)),
                type.getSingleFormat(property.pattern).getPattern(), false);

        popup.setVisible(false);
        popup.addAutoHidePartner(getPickerElement());
        editBox.click(); // need to dateRangePicker opens immediately. because we use an editBox
        return new SimplePanel();
    }

    @Override
    public Object getDefaultNullValue() {
        return type.fromDate(new Date(), new Date());
    }

    protected void pickerApply(Element parent) {
        setInputValue(type.fromDate(GwtClientUtils.fromJsDate(getStartDate()), GwtClientUtils.fromJsDate(getEndDate())));
        popup.hide();
        commit(parent, CommitReason.BLURRED);
    }

    protected native void removePicker()/*-{
        $(this.@TextBasedPopupCellEditor::editBox).data('daterangepicker').remove();
    }-*/;

    protected native Element getPickerElement()/*-{
        return $(this.@TextBasedPopupCellEditor::editBox).data('daterangepicker').container.get(0);
    }-*/;

    private native JsDate getStartDate()/*-{
        var pickerDate = $(this.@TextBasedPopupCellEditor::editBox).data('daterangepicker').startDate;
        return pickerDate.isValid() ? pickerDate.toDate() : null; // toDate because it is "Moment js" object
    }-*/;

    private native JsDate getEndDate()/*-{
        var pickerDate = $(this.@TextBasedPopupCellEditor::editBox).data('daterangepicker').endDate;
        return pickerDate.isValid() ? pickerDate.toDate() : null; // toDate because it is "Moment js" object
    }-*/;

    protected native void createPicker(Element parent, JsDate startDate, JsDate endDate, String pattern, boolean singleDatePicker)/*-{
        window.$ = $wnd.jQuery;
        var thisObj = this;
        var editElement = $(thisObj.@TextBasedPopupCellEditor::editBox);
        var time = thisObj.@IntervalCellEditor::intervalType === 'TIME';
        var date = thisObj.@IntervalCellEditor::intervalType === 'DATE';
        var messages = @lsfusion.gwt.client.ClientMessages.Instance::get()();

        var format = pattern.replaceAll("d", "D").replaceAll("y", "Y").replaceAll("a", "A"); // dateRangePicker format - date uses capital letters, time uses small letters, AM/PM uses capital letter

        editElement.daterangepicker({
            locale: {
                applyLabel: messages.@lsfusion.gwt.client.ClientMessages::applyLabel()(),
                cancelLabel: messages.@lsfusion.gwt.client.ClientMessages::cancelLabel()(),
                customRangeLabel: messages.@lsfusion.gwt.client.ClientMessages::customRangeLabel()(),
                daysOfWeek: [
                    messages.@lsfusion.gwt.client.ClientMessages::daysOfWeekSU()(),
                    messages.@lsfusion.gwt.client.ClientMessages::daysOfWeekMO()(),
                    messages.@lsfusion.gwt.client.ClientMessages::daysOfWeekTU()(),
                    messages.@lsfusion.gwt.client.ClientMessages::daysOfWeekWE()(),
                    messages.@lsfusion.gwt.client.ClientMessages::daysOfWeekTH()(),
                    messages.@lsfusion.gwt.client.ClientMessages::daysOfWeekFR()(),
                    messages.@lsfusion.gwt.client.ClientMessages::daysOfWeekSA()()
                ],
                monthNames: [
                    messages.@lsfusion.gwt.client.ClientMessages::monthJanuary()(),
                    messages.@lsfusion.gwt.client.ClientMessages::monthFebruary()(),
                    messages.@lsfusion.gwt.client.ClientMessages::monthMarch()(),
                    messages.@lsfusion.gwt.client.ClientMessages::monthApril()(),
                    messages.@lsfusion.gwt.client.ClientMessages::monthMay()(),
                    messages.@lsfusion.gwt.client.ClientMessages::monthJune()(),
                    messages.@lsfusion.gwt.client.ClientMessages::monthJuly()(),
                    messages.@lsfusion.gwt.client.ClientMessages::monthAugust()(),
                    messages.@lsfusion.gwt.client.ClientMessages::monthSeptember()(),
                    messages.@lsfusion.gwt.client.ClientMessages::monthOctober()(),
                    messages.@lsfusion.gwt.client.ClientMessages::monthNovember()(),
                    messages.@lsfusion.gwt.client.ClientMessages::monthDecember()()
                ],
                "firstDay": 1,
                format: format // need to be able to enter date from keyboard
            },
            startDate: startDate,
            endDate: endDate,
            timePicker: !date,
            timePicker24Hour: true,
            showDropdowns: true,
            autoApply: false,
            ranges: !time ? $wnd.getRanges($wnd, messages.@lsfusion.gwt.client.ClientMessages::today()(),
                    messages.@lsfusion.gwt.client.ClientMessages::yesterday()(),
                    messages.@lsfusion.gwt.client.ClientMessages::last7Days()(),
                    messages.@lsfusion.gwt.client.ClientMessages::last30Days()(),
                    messages.@lsfusion.gwt.client.ClientMessages::thisMonth()(),
                    messages.@lsfusion.gwt.client.ClientMessages::lastMonth()(),
                    messages.@lsfusion.gwt.client.ClientMessages::clear()()): undefined,
            singleDatePicker: singleDatePicker,
            autoUpdateInput: false, // update the parent component only after pressing "apply" and not when hiding
            alwaysShowCalendars: true // need to use with ranges
        });

        //show only time picker
        if (time) {
            editElement.on('show.daterangepicker', function (ev, picker) {
                picker.container.find(".calendar-table").hide();
                picker.container.find(".drp-selected").hide();
            })
        }

        editElement.on('cancel.daterangepicker', function () {
            thisObj.@IntervalCellEditor::cancel(Lcom/google/gwt/dom/client/Element;)(parent);
        });
        editElement.on('apply.daterangepicker', function () {
            thisObj.@IntervalCellEditor::pickerApply(*)(parent);
        });
    }-*/;
}
