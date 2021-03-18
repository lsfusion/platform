package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.classes.data.GIntervalType;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import java.math.BigDecimal;
import java.util.Date;

public class IntervalCellEditor implements CellEditor {

    private final EditManager editManager;
    private final String intervalType;
    private static final ClientMessages messages = ClientMessages.Instance.get();

    public IntervalCellEditor(EditManager editManager, String intervalType) {
        this.editManager = editManager;
        this.intervalType = intervalType;
    }

    public void validateAndCommit(Long dateFrom, Long dateTo) {
        if (dateFrom != null && dateTo != null)
            editManager.commitEditing(new BigDecimal(dateFrom + "." + dateTo));
        else
            editManager.cancelEditing();
    }

    @Override
    public void startEditing(Event event, Element parent, Object oldValue) {
        String object = String.valueOf(oldValue);
        int indexOfDecimal = object.indexOf(".");

        createPicker(parent, GIntervalType.getTimestamp(object.substring(0, indexOfDecimal)),
                GIntervalType.getTimestamp(object.substring(indexOfDecimal + 1)), intervalType, false);
    }
    protected static native void getLocalizedString(String string)/*-{
        var name;
        var prototype = Object.getPrototypeOf(@IntervalCellEditor::messages);
        var ownPropertyNames = Object.getOwnPropertyNames(prototype);
        for (var i = 0; i < ownPropertyNames.length; i++) {
            var property = ownPropertyNames[i];
            if (property.includes(string)) {
                name = property;
                break;
            }
        }
        return name != null ? prototype[name]() : name;
    }-*/;

    protected native void createPicker(Element parent, Date startDate, Date endDate, String intervalType, boolean singleDatePicker)/*-{
        window.$ = $wnd.jQuery;
        var parentEl = $(parent);
        var thisObj = this;
        var time = intervalType === 'TIME';
        var date = intervalType === 'DATE';

        parentEl.daterangepicker({
            locale: {
                applyLabel: @IntervalCellEditor::getLocalizedString(*)("applyLabel"),
                cancelLabel: @IntervalCellEditor::getLocalizedString(*)("cancelLabel"),
                customRangeLabel: @IntervalCellEditor::getLocalizedString(*)("customRangeLabel"),
                daysOfWeek: [
                    @IntervalCellEditor::getLocalizedString(*)("daysOfWeekSU"),
                    @IntervalCellEditor::getLocalizedString(*)("daysOfWeekMO"),
                    @IntervalCellEditor::getLocalizedString(*)("daysOfWeekTU"),
                    @IntervalCellEditor::getLocalizedString(*)("daysOfWeekWE"),
                    @IntervalCellEditor::getLocalizedString(*)("daysOfWeekTH"),
                    @IntervalCellEditor::getLocalizedString(*)("daysOfWeekFR"),
                    @IntervalCellEditor::getLocalizedString(*)("daysOfWeekSA")
                ],
                monthNames: [
                    @IntervalCellEditor::getLocalizedString(*)("monthJanuary"),
                    @IntervalCellEditor::getLocalizedString(*)("monthFebruary"),
                    @IntervalCellEditor::getLocalizedString(*)("monthMarch"),
                    @IntervalCellEditor::getLocalizedString(*)("monthApril"),
                    @IntervalCellEditor::getLocalizedString(*)("monthMay"),
                    @IntervalCellEditor::getLocalizedString(*)("monthJune"),
                    @IntervalCellEditor::getLocalizedString(*)("monthJuly"),
                    @IntervalCellEditor::getLocalizedString(*)("monthAugust"),
                    @IntervalCellEditor::getLocalizedString(*)("monthSeptember"),
                    @IntervalCellEditor::getLocalizedString(*)("monthOctober"),
                    @IntervalCellEditor::getLocalizedString(*)("monthNovember"),
                    @IntervalCellEditor::getLocalizedString(*)("monthDecember")
                ],
                "firstDay": 1
            },
            startDate: new Date(startDate),
            endDate: new Date(endDate),
            timePicker: !date,
            timePicker24Hour: true,
            autoApply: false,
            ranges: !time ? $wnd.getRanges($wnd, @IntervalCellEditor::getLocalizedString(*)("today"),
                @IntervalCellEditor::getLocalizedString(*)("yesterday"),
                @IntervalCellEditor::getLocalizedString(*)("last7Days"),
                @IntervalCellEditor::getLocalizedString(*)("last30Days"),
                @IntervalCellEditor::getLocalizedString(*)("thisMonth"),
                @IntervalCellEditor::getLocalizedString(*)("lastMonth")) : undefined,
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
            var dateFrom = startDate != null ? startDate.unix() : null;
            var dateTo = endDate != null ? endDate.unix() : null;
            thisObj.@lsfusion.gwt.client.form.property.cell.classes.controller.IntervalCellEditor::validateAndCommit(*)(dateFrom, dateTo);
        });

    }-*/;
}
