package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.classes.data.GIntervalType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import java.math.BigDecimal;
import java.util.Date;

public class IntervalCellEditor extends PopupValueCellEditor {

    private final String intervalType;
    private final GIntervalType interval;

    public IntervalCellEditor(EditManager editManager, GPropertyDraw property, String intervalType, GIntervalType interval) {
        super(editManager, property);
        this.intervalType = intervalType;
        this.interval = interval;
    }

    @Override
    public Object getValue(Element parent, Integer contextAction) {
        String value = getPickerValue(parent, intervalType);
        return value != null ? new BigDecimal(value) : null;
    }

    protected native String getPickerValue(Element parent, String intervalType)/*-{
        var picker = $(parent).data('daterangepicker');

        var startDate = picker.startDate;
        var endDate = picker.endDate;
        var dateFrom = startDate.isValid() ? (intervalType === 'ZDATETIME' ? startDate.unix() :
            Date.UTC(startDate.year(), startDate.month(), startDate.date(), startDate.hour(), startDate.minute(), startDate.second()) / 1000) : null;
        var dateTo = endDate.isValid() != null ? (intervalType === 'ZDATETIME' ? endDate.unix() :
            Date.UTC(endDate.year(), endDate.month(), endDate.date(), endDate.hour(), endDate.minute(), endDate.second()) / 1000) : null;

        return dateFrom == null || dateTo == null ? null : dateFrom + '.' + dateTo;
    }-*/;

    @Override
    public void stop(Element parent, boolean cancel) {
        popup.removeAutoHidePartner(getPickerElement(parent));
        removePicker(parent);
        super.stop(parent, cancel);
    }

    protected native void removePicker(JavaScriptObject parent)/*-{
        $(parent).data('daterangepicker').remove();
    }-*/;

    @Override
    protected Widget createPopupComponent(Element parent, Object oldValue) {
        createPicker(parent, interval.getDate(oldValue, true), interval.getDate(oldValue, false), intervalType, false);
        popup.addAutoHidePartner(getPickerElement(parent));
        popup.setVisible(false);

        //return fake widget
        return new SimplePanel();
    }

    protected native Element getPickerElement(Element parent)/*-{
        return $(parent).data('daterangepicker').container.get(0);
    }-*/;

    protected native void createPicker(Element parent, Date startDate, Date endDate, String intervalType, boolean singleDatePicker)/*-{
        window.$ = $wnd.jQuery;
        var parentEl = $(parent);
        var thisObj = this;
        var time = intervalType === 'TIME';
        var date = intervalType === 'DATE';
        var messages = @lsfusion.gwt.client.ClientMessages.Instance::get()();

        parentEl.daterangepicker({
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
                "firstDay": 1
            },
            startDate: new Date(startDate),
            endDate: new Date(endDate),
            timePicker: !date,
            timePicker24Hour: true,
            autoApply: false,
            ranges: !time ? $wnd.getRanges($wnd, messages.@lsfusion.gwt.client.ClientMessages::today()(),
                    messages.@lsfusion.gwt.client.ClientMessages::yesterday()(),
                    messages.@lsfusion.gwt.client.ClientMessages::last7Days()(),
                    messages.@lsfusion.gwt.client.ClientMessages::last30Days()(),
                    messages.@lsfusion.gwt.client.ClientMessages::thisMonth()(),
                    messages.@lsfusion.gwt.client.ClientMessages::lastMonth()(),
                    messages.@lsfusion.gwt.client.ClientMessages::clear()()): undefined,
            singleDatePicker: singleDatePicker,
            alwaysShowCalendars: true // need to use with ranges
        });

        //show only time picker
        if (time) {
            parentEl.on('show.daterangepicker', function (ev, picker) {
                picker.container.find(".calendar-table").hide();
                picker.container.find(".drp-selected").hide();
            })
        }

        parentEl.on('hide.daterangepicker', function () {
            thisObj.@IntervalCellEditor::commit(*)(parent, @lsfusion.gwt.client.form.property.cell.controller.CommitReason::BLURRED);
        });

    }-*/;
}
