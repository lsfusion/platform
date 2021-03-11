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
                GIntervalType.getTimestamp(object.substring(indexOfDecimal + 1)), intervalType);
    }

    protected native void createPicker(Element parent, Date startDate, Date endDate, String intervalType)/*-{
        window.$ = $wnd.jQuery;
        var parentEl = $(parent);
        var thisObj = this;
        var time = intervalType === 'time';
        var date = intervalType === 'date';

        parentEl.daterangepicker({
            startDate: new Date(startDate),
            endDate: new Date(endDate),
            timePicker: !date,
            timePicker24Hour: true,
            autoApply: false,
            ranges: !time ? {
                'Today': [$wnd.moment(), $wnd.moment()],
                'Yesterday': [$wnd.moment().subtract(1, 'days'), $wnd.moment().subtract(1, 'days')],
                'Last 7 Days': [$wnd.moment().subtract(6, 'days'), $wnd.moment()],
                'Last 30 Days': [$wnd.moment().subtract(29, 'days'), $wnd.moment()],
                'This Month': [$wnd.moment().startOf('month'), $wnd.moment().endOf('month')],
                'Last Month': [$wnd.moment().subtract(1, 'month').startOf('month'), $wnd.moment().subtract(1, 'month').endOf('month')]
            } : undefined,
//            singleDatePicker: true
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
