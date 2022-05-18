package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JsDate;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.SimplePanel;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import java.text.ParseException;

public abstract class DateRangePickerBasedCellEditor extends TextBasedPopupCellEditor implements FormatCellEditor {

    private final boolean isSinglePicker;

    public DateRangePickerBasedCellEditor(EditManager editManager, GPropertyDraw property, boolean isSinglePicker) {
        super(editManager, property);
        this.isSinglePicker = isSinglePicker;
    }

    @Override
    public void stop(Element parent, boolean cancel, boolean blurred) {
        removePicker();
        super.stop(parent, cancel, blurred);
    }

    protected void pickerApply(Element parent) {
        setInputValue(getInputValue());
        popup.hide();
        commit(parent, CommitReason.BLURRED);
    }

    private boolean commit = false;
    protected Object tryParseInputText(String inputText, boolean onCommit) throws ParseException {
        //to be able to enter the date from keyboard
        if (commit) {
            try {
                return super.tryParseInputText(inputText, onCommit);
            } catch (ParseException e) {
                commit = false;
                throw e;
            }
        }
        return inputText;
    }

    @Override
    public void commit(Element parent, CommitReason commitReason) {
        commit = true;
        super.commit(parent, commitReason);
    }

    @Override
    public SimplePanel createPopupComponent(Element parent, Object oldValue) {
        assert oldValue != null;
        createPicker(parent, getStartDate(oldValue), getEndDate(oldValue), getPattern(), isSinglePicker, isTimeEditor(), isDateEditor());

        popup.setVisible(false);
        popup.addAutoHidePartner(getPickerElement());
        editBox.click(); // need to dateRangePicker opens immediately. because we use an editBox
        return new SimplePanel();
    }

    protected abstract JsDate getStartDate(Object oldValue);
    protected abstract JsDate getEndDate(Object oldValue);
    protected abstract String getPattern();
    protected abstract boolean isTimeEditor();
    protected abstract boolean isDateEditor();
    protected abstract Object getInputValue();

    protected native void removePicker()/*-{
        $(this.@TextBasedPopupCellEditor::editBox).data('daterangepicker').remove();
    }-*/;

    protected native Element getPickerElement()/*-{
        return $(this.@TextBasedPopupCellEditor::editBox).data('daterangepicker').container.get(0);
    }-*/;

    protected native JsDate getPickerStartDate()/*-{
        var pickerDate = $(this.@TextBasedPopupCellEditor::editBox).data('daterangepicker').startDate;
        return pickerDate.isValid() ? pickerDate.toDate() : null; // toDate because it is "Moment js" object
    }-*/;

    protected native JsDate getPickerEndDate()/*-{
        var pickerDate = $(this.@TextBasedPopupCellEditor::editBox).data('daterangepicker').endDate;
        return pickerDate.isValid() ? pickerDate.toDate() : null; // toDate because it is "Moment js" object
    }-*/;

    protected native void createPicker(Element parent, JsDate startDate, JsDate endDate, String pattern, boolean singleDatePicker, boolean time, boolean date)/*-{
        window.$ = $wnd.jQuery;
        var thisObj = this;
        var editElement = $(thisObj.@TextBasedPopupCellEditor::editBox);
        var messages = @lsfusion.gwt.client.ClientMessages.Instance::get()();

        var format = pattern.replaceAll("d", "D").replaceAll("y", "Y").replaceAll("a", "A"); // dateRangePicker format - date uses capital letters, time uses small letters, AM/PM uses capital letter

        //Must be called before the picker is initialised, or its events will be triggered earlier
        editElement.on('keydown.daterangepicker', function (e) {
            if (("key" in e && e.key === "Escape" || e.key === "Esc") || e.keyCode === 27) {
                thisObj.@ARequestValueCellEditor::cancel(Lcom/google/gwt/dom/client/Element;Llsfusion/gwt/client/form/property/cell/controller/CancelReason;)(parent, @lsfusion.gwt.client.form.property.cell.controller.CancelReason::ESCAPE_PRESSED);
            } else if ((e.keyCode === 9) || (e.keyCode === 13)) {
                //For picker does not close on pressing enter. We will close it ourselves in the commit method. stopPropagation() and preventDefault() does not work;
                e.keyCode = 0;
            }
        });

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
            autoApply: true,
            ranges: !time && !singleDatePicker ? $wnd.getRanges($wnd, messages.@lsfusion.gwt.client.ClientMessages::today()(),
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
            });
        }

        //Return focus to editElement and then we will handle the press of the esc button. Because daterangepicker does not allow to handle events
        $(thisObj.@DateRangePickerBasedCellEditor::getPickerElement()()).on('mouseup keyup', function () {
            editElement.focus();
            var input = editElement.get(0);
            input.selectionStart = input.selectionEnd = input.value.length; //To place the cursor at the very end
        });

        editElement.on('cancel.daterangepicker', function () {
            thisObj.@lsfusion.gwt.client.form.property.cell.classes.controller.DateRangePickerBasedCellEditor::cancel(Lcom/google/gwt/dom/client/Element;)(parent);
        });

        editElement.on('apply.daterangepicker', function () {
            thisObj.@lsfusion.gwt.client.form.property.cell.classes.controller.DateRangePickerBasedCellEditor::pickerApply(*)(parent);
        });
    }-*/;
}
