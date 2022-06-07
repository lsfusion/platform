package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JsDate;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.SimplePanel;
import lsfusion.gwt.client.controller.SmartScheduler;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import java.text.ParseException;

public abstract class DateRangePickerBasedCellEditor extends TextBasedPopupCellEditor implements FormatCellEditor {

    public DateRangePickerBasedCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
    }

    @Override
    public void stop(Element parent, boolean cancel, boolean blurred) {
        removePicker();
        super.stop(parent, cancel, blurred);
    }

    protected void pickerApply(Element parent) {
        //when auto-apply selects a date, at the end a mousedown occurs and takes the focus to a nothing
        SmartScheduler.getInstance().scheduleDeferred(true, () -> {
            if (editManager.isThisCellEditing(this))
                commitValue(parent, getInputValue());
        });
    }

    protected Object tryParseInputText(String inputText, boolean onCommit) throws ParseException {
        //to be able to enter the date from keyboard
        return onCommit ? super.tryParseInputText(inputText, true) : inputText;
    }

    @Override
    public SimplePanel createPopupComponent(Element parent, Object oldValue) {
        assert oldValue != null;
        createPicker(parent, getStartDate(oldValue), getEndDate(oldValue), getPattern(), isSinglePicker(), isTimeEditor(), isDateEditor());

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
    protected abstract boolean isSinglePicker();

    protected native void removePicker()/*-{
        $(this.@TextBasedPopupCellEditor::editBox).data('daterangepicker').remove();
        //we need to remove the keydown listener because it is a global($wnd) listener that is only used when the picker popup opens
        $($wnd).off('keydown.pickerpopup');
    }-*/;

    protected native Element getPickerElement()/*-{
        return $(this.@TextBasedPopupCellEditor::editBox).data('daterangepicker').container.get(0);
    }-*/;

    protected native JsDate getPickerStartDate(boolean rawDate)/*-{
        var pickerDate = $(this.@TextBasedPopupCellEditor::editBox).data('daterangepicker').startDate;
        return pickerDate.isValid() ? rawDate ? pickerDate : pickerDate.toDate() : null; // toDate because it is "Moment js" object
    }-*/;

    protected native JsDate getPickerEndDate(boolean rawDate)/*-{
        var pickerDate = $(this.@TextBasedPopupCellEditor::editBox).data('daterangepicker').endDate;
        // pickerDate may be null because we update the input field and on select 'date_from' - 'date_to' will be null
        pickerDate = pickerDate == null ? this.@DateRangePickerBasedCellEditor::getPickerStartDate(*)(true) : pickerDate;
        return pickerDate.isValid() ? rawDate ? pickerDate : pickerDate.toDate() : null; // toDate because it is "Moment js" object
    }-*/;

    protected native void createPicker(Element parent, JsDate startDate, JsDate endDate, String pattern, boolean singleDatePicker, boolean time, boolean date)/*-{
        window.$ = $wnd.jQuery;
        var thisObj = this;
        var editElement = $(thisObj.@TextBasedPopupCellEditor::editBox);
        var messages = @lsfusion.gwt.client.ClientMessages.Instance::get()();

        var format = pattern.replaceAll("d", "D").replaceAll("y", "Y").replaceAll("a", "A"); // dateRangePicker format - date uses capital letters, time uses small letters, AM/PM uses capital letter

        //Must be called before the picker is initialised, or its events will be triggered earlier
        //override of the datepicker.keydown method. Copied from daterangepicker.js with some changes
        $wnd.daterangepicker.prototype.keydown = function (e) {
            //hide on esc and prevent propagation
            if (e.keyCode === 27) {
                e.preventDefault();
                e.stopPropagation();
                thisObj.@ARequestValueCellEditor::cancel(Lcom/google/gwt/dom/client/Element;Llsfusion/gwt/client/form/property/cell/controller/CancelReason;)(parent, @lsfusion.gwt.client.form.property.cell.controller.CancelReason::ESCAPE_PRESSED);
            }
        }

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
            ranges: !time ? $wnd[singleDatePicker ? 'getSingleRanges' : 'getRanges']($wnd, messages.@lsfusion.gwt.client.ClientMessages::today()(),
                messages.@lsfusion.gwt.client.ClientMessages::yesterday()(),
                singleDatePicker ? messages.@lsfusion.gwt.client.ClientMessages::sevenDaysAgo()() : messages.@lsfusion.gwt.client.ClientMessages::last7Days()(),
                singleDatePicker ? messages.@lsfusion.gwt.client.ClientMessages::thirtyDaysAgo()() : messages.@lsfusion.gwt.client.ClientMessages::last30Days()(),
                singleDatePicker ? messages.@lsfusion.gwt.client.ClientMessages::monthStart()() : messages.@lsfusion.gwt.client.ClientMessages::thisMonth()(),
                singleDatePicker ? messages.@lsfusion.gwt.client.ClientMessages::previousMonthStart()() : messages.@lsfusion.gwt.client.ClientMessages::previousMonth()(),
                singleDatePicker ? messages.@lsfusion.gwt.client.ClientMessages::thisYearStart()() : messages.@lsfusion.gwt.client.ClientMessages::thisYear()(),
                messages.@lsfusion.gwt.client.ClientMessages::clear()()) : undefined,
            singleDatePicker: singleDatePicker,
            drops: 'auto',
            opens: getPickerAlign(),
            alwaysShowCalendars: true // need to use with ranges
        });

        //show only time picker
        if (time) {
            editElement.on('show.daterangepicker', function (ev, picker) {
                var pickerContainer = picker.container;
                var calendarTables = pickerContainer.find(".calendar-table");
                var offsetHeight = calendarTables.get(0).offsetHeight;

                //determinate horizontal or vertical(for small screen size) interval picker
                var calendarTablesOffsetHeight = pickerContainer.height() < offsetHeight * 2 ? offsetHeight : offsetHeight * 2;

                calendarTables.hide();
                pickerContainer.find(".drp-selected").hide();

                //because we hide calendar tables when shown "drop-up" only timepicker it is shown in wrong place
                if (pickerContainer.hasClass("drop-up")) {
                    var pickerElement = pickerContainer.get(0);
                    pickerElement.style.top = (pickerElement.offsetTop + calendarTablesOffsetHeight - parseInt(window.getComputedStyle(pickerElement).marginTop)) + "px";
                }
            });
        }

        //swap 'left' and 'right' because dateRangePicker library swap them inexplicably for what
        function getPickerAlign() {
            var propertyHorTextAlignment = thisObj.@TextBasedCellEditor::property.@GPropertyDraw::getCellRenderer()()
                .@lsfusion.gwt.client.form.property.cell.view.CellRenderer::getHorzTextAlignment()();
            propertyHorTextAlignment = propertyHorTextAlignment === @com.google.gwt.dom.client.Style.TextAlign::LEFT ? @com.google.gwt.dom.client.Style.TextAlign::RIGHT
                : propertyHorTextAlignment === @com.google.gwt.dom.client.Style.TextAlign::RIGHT ? @com.google.gwt.dom.client.Style.TextAlign::LEFT : propertyHorTextAlignment;
            return propertyHorTextAlignment.@com.google.gwt.dom.client.Style.TextAlign::getCssName()();
        }

        //update input element
        $(thisObj.@DateRangePickerBasedCellEditor::getPickerElement()()).on('mouseup keyup change.daterangepicker', function (e) {
            if (e.target.tagName !== 'SELECT' || e.type !== 'mouseup') {
                if (singleDatePicker) {
                    editElement.val(thisObj.@DateRangePickerBasedCellEditor::getPickerStartDate(*)(true).format(format));
                } else {
                    var startDate = thisObj.@DateRangePickerBasedCellEditor::getPickerStartDate(*)(true);
                    var endDate = thisObj.@DateRangePickerBasedCellEditor::getPickerEndDate(*)(true);
                    editElement.val(startDate.format(format) + (endDate != null ? ' - ' + endDate.format(format) : ''));
                }
            }
        });

        editElement.on('cancel.daterangepicker', function () {
            thisObj.@lsfusion.gwt.client.form.property.cell.classes.controller.DateRangePickerBasedCellEditor::cancel(Lcom/google/gwt/dom/client/Element;)(parent);
        });

        editElement.on('apply.daterangepicker', function () {
            thisObj.@lsfusion.gwt.client.form.property.cell.classes.controller.DateRangePickerBasedCellEditor::pickerApply(*)(parent);
        });

        editElement.on('show.daterangepicker', function () {
            $($wnd).on('keydown.pickerpopup', function (e) {
                if (e.keyCode === 27)
                    thisObj.@ARequestValueCellEditor::cancel(Lcom/google/gwt/dom/client/Element;Llsfusion/gwt/client/form/property/cell/controller/CancelReason;)(parent, @lsfusion.gwt.client.form.property.cell.controller.CancelReason::ESCAPE_PRESSED);
                else if ((e.keyCode === 9) || (e.keyCode === 13))
                    thisObj.@lsfusion.gwt.client.form.property.cell.classes.controller.DateRangePickerBasedCellEditor::pickerApply(*)(parent);
            });
        });

        editElement.on('hide.daterangepicker', function () {
            $($wnd).off('keydown.pickerpopup');
        });
    }-*/;
}
