package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.classes.data.GIntervalType;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import java.math.BigDecimal;
import java.util.Date;

public class IntervalCellEditor implements CellEditor {

    private final EditManager editManager;
    private final String intervalType;
    private final GIntervalType interval;

    public IntervalCellEditor(EditManager editManager, String intervalType, GIntervalType interval) {
        this.editManager = editManager;
        this.intervalType = intervalType;
        this.interval = interval;
    }

    public void validateAndCommit(Long dateFrom, Long dateTo) {
        if (dateFrom != null && dateTo != null)
            editManager.commitEditing(new BigDecimal(dateFrom + "." + dateTo));
        else if (dateFrom == null && dateTo == null)
            editManager.commitEditing(null);
        else
            editManager.cancelEditing();
    }

    @Override
    public void startEditing(Event event, Element parent, Object oldValue) {
        createPicker(parent, getObjectDate(oldValue, true), getObjectDate(oldValue, false), intervalType, false);
    }

    private Date getObjectDate(Object oldValue, boolean from) {
        String object = String.valueOf(oldValue);
        int indexOfDecimal = object.indexOf(".");
        long epoch = indexOfDecimal < 0 ? new Date().getTime() : Long.parseLong(from ? object.substring(0, indexOfDecimal) : object.substring(indexOfDecimal + 1));
        return interval.getFormat(null).parse(interval.format(epoch));
    }

    protected native void createPicker(Element parent, Date startDate, Date endDate, String intervalType, boolean singleDatePicker)/*-{
        window.$ = $wnd.jQuery;
        var parentEl = $(parent);
        var thisObj = this;
        var time = intervalType === 'TIME';
        var date = intervalType === 'DATE';

        parentEl.daterangepicker({
            locale: {
                applyLabel: @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("applyLabel"),
                cancelLabel: @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("cancelLabel"),
                customRangeLabel: @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("customRangeLabel"),
                daysOfWeek: [
                    @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("daysOfWeekSU"),
                    @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("daysOfWeekMO"),
                    @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("daysOfWeekTU"),
                    @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("daysOfWeekWE"),
                    @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("daysOfWeekTH"),
                    @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("daysOfWeekFR"),
                    @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("daysOfWeekSA")
                ],
                monthNames: [
                    @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("monthJanuary"),
                    @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("monthFebruary"),
                    @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("monthMarch"),
                    @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("monthApril"),
                    @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("monthMay"),
                    @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("monthJune"),
                    @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("monthJuly"),
                    @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("monthAugust"),
                    @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("monthSeptember"),
                    @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("monthOctober"),
                    @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("monthNovember"),
                    @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("monthDecember")
                ],
                "firstDay": 1
            },
            startDate: new Date(startDate),
            endDate: new Date(endDate),
            timePicker: !date,
            timePicker24Hour: true,
            autoApply: false,
            ranges: !time ? $wnd.getRanges($wnd, @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("today"),
                @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("yesterday"),
                @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("last7Days"),
                @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("last30Days"),
                @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("thisMonth"),
                @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("lastMonth"),
                @lsfusion.gwt.client.base.GwtClientUtils::getLocalizedString(*)("clear")): undefined,
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

        parentEl.on('hide.daterangepicker', function (ev, picker) {
            var startDate = picker.startDate;
            var endDate = picker.endDate;
            var epochFrom = Math.floor(startDate.toDate().getTime() / 1000);
            var epochTo = Math.floor(endDate.toDate().getTime() / 1000);
            var timezoneOffset = new Date().getTimezoneOffset();
            var dateFrom = intervalType === 'ZDATETIME' ? epochFrom : (epochFrom - timezoneOffset * 60);
            var dateTo = intervalType === 'ZDATETIME' ? epochTo : (epochTo - timezoneOffset * 60);
            thisObj.@lsfusion.gwt.client.form.property.cell.classes.controller.IntervalCellEditor::validateAndCommit(*)(dateFrom, dateTo);
        });

    }-*/;
}
