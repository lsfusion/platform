package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.SimplePanel;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.classes.data.GIntervalType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;

public class IntervalCellEditor extends TextBasedPopupCellEditor {

    private final String intervalType;
    private final GIntervalType interval;

    public IntervalCellEditor(EditManager editManager, GPropertyDraw property, String intervalType, GIntervalType interval) {
        super(editManager, property);
        this.intervalType = intervalType;
        this.interval = interval;
    }

    @Override
    protected Object parseString(String value) throws ParseException {
        try {
            return value.isEmpty() ? null : new BigDecimal(getEpoch(value)); // value can be empty when the "clear" button in dateRangePicker is pressed
        } catch (NumberFormatException e) {
            return RequestValueCellEditor.invalid;
        }
    }

    protected native String getEpoch(String value)/*-{
        var startDate, endDate;
        if (value != null) {
            var split = value.split(" - ");
            startDate = new Date(split[0]);
            endDate = new Date(split[1]);
        } else {
            var picker = $(this.@TextBasedPopupCellEditor::editBox).data('daterangepicker');
            var pickerStartDate = picker.startDate;
            var pickerEndDate = picker.endDate;
            startDate = pickerStartDate.isValid() ? pickerStartDate.toDate() : null; // toDate because it is "Moment js" object
            endDate = pickerEndDate.isValid() ? picker.endDate.toDate() : null;
        }

        var dateFrom = startDate != null ? (this.@IntervalCellEditor::intervalType === 'ZDATETIME' ? startDate.getTime() / 1000 :
            Date.UTC(startDate.getFullYear(), startDate.getMonth(), startDate.getDate(), startDate.getHours(), startDate.getMinutes(), startDate.getSeconds()) / 1000) : null;
        var dateTo = endDate != null ? (this.@IntervalCellEditor::intervalType === 'ZDATETIME' ? endDate.getTime() / 1000 :
            Date.UTC(endDate.getFullYear(), endDate.getMonth(), endDate.getDate(), endDate.getHours(), endDate.getMinutes(), endDate.getSeconds()) / 1000) : null;

        return dateFrom == null || dateTo == null ? null : dateFrom + '.' + dateTo;
    }-*/;

    @Override
    public void clearRender(Element cellParent, RenderContext renderContext, boolean cancel) {
        super.clearRender(cellParent, renderContext, cancel);
        removePicker();
    }

    protected native void removePicker()/*-{
        $(this.@TextBasedPopupCellEditor::editBox).data('daterangepicker').remove();
    }-*/;

    @Override
    public void start(Event editEvent, Element parent, Object oldValue) {
        super.start(editEvent, parent, oldValue);

        createPicker(parent, interval.getDate(oldValue, true), interval.getDate(oldValue, false), false);
        popup.addAutoHidePartner(getPickerElement());
        popup.setVisible(false);

        if (oldValue != null)
            editBox.setValue(interval.formatObject(oldValue));

        GwtClientUtils.showPopupInWindow(popup, new SimplePanel(), parent.getAbsoluteLeft(), parent.getAbsoluteBottom());
        editBox.click(); // need to dateRangePicker opens immediately. because we use an editBox
    }

    protected void pickerApply(Element parent) {
        String pickerValue = getEpoch(null);
        editBox.setValue(pickerValue != null ? interval.formatObject(pickerValue) : null);
        popup.hide();
        commit(parent, CommitReason.BLURRED);
    }

    protected native Element getPickerElement()/*-{
        return $(this.@TextBasedPopupCellEditor::editBox).data('daterangepicker').container.get(0);
    }-*/;

    protected native void createPicker(Element parent, Date startDate, Date endDate, boolean singleDatePicker)/*-{
        window.$ = $wnd.jQuery;
        var thisObj = this;
        var editElement = $(thisObj.@TextBasedPopupCellEditor::editBox);
        var time = thisObj.@IntervalCellEditor::intervalType === 'TIME';
        var date = thisObj.@IntervalCellEditor::intervalType === 'DATE';
        var messages = @lsfusion.gwt.client.ClientMessages.Instance::get()();

        var format = @lsfusion.gwt.client.base.GwtSharedUtils::getDateTimeFormat(*)(thisObj.@TextBasedCellEditor::property.@lsfusion.gwt.client.form.property.GPropertyDraw::pattern, false)
            .@com.google.gwt.i18n.shared.DateTimeFormat::getPattern()()
                .replaceAll("d", "D").replaceAll("y", "Y").replaceAll("a", "A"); // dateRangePicker format - date uses capital letters, time uses small letters, AM/PM uses capital letter

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
            startDate: new Date(startDate),
            endDate: new Date(endDate),
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
