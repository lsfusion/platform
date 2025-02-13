package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsDate;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.SimpleDatePatternConverter;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

import java.text.ParseException;

public abstract class DateRangePickerBasedCellEditor extends TextBasedCellEditor implements FormatCellEditor {

    public DateRangePickerBasedCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
    }

    @Override
    public boolean startText(EventHandler handler, Element parent, RenderContext renderContext, PValue oldValue) {
        boolean explicitValue = super.startText(handler, parent, renderContext, oldValue);
        if (started && !isNative()) {
            createPicker(parent, oldValue != null ? getStartDate(oldValue) : null, oldValue != null ? getEndDate(oldValue) : null,
                    getSinglePattern().replace("a", "A"), isSinglePicker(), isTimeEditor(), isDateEditor(),
                    !explicitValue && oldValue == null);
            openPicker(); // date range picker is opened only on click

            GwtClientUtils.addDropDownPartner(parent, getPickerContainer());
        }

        return explicitValue;
    }

    @Override
    public void stop(Element parent, boolean cancel, boolean blurred) {
        if(started && !isNative())
            removePicker();
        super.stop(parent, cancel, blurred);
    }

    protected void pickerApply(Element parent) {
        commit(parent);
    }

    protected void pickerCancel() {
        cancel();
    }

    @Override
    public PValue getCommitValue(Element parent, Integer contextAction) throws InvalidEditException {
        if(!isNative()) // needed for the 2-year digit dates
            return getValue(getPickerStartDate(), getPickerEndDate());
        return super.getCommitValue(parent, contextAction);
    }

    protected PValue tryParseInputText(String inputText, boolean onCommit) throws ParseException {
        //to be able to enter the date from keyboard
        if (!onCommit)
            return PValue.getPValue(inputText);

        return super.tryParseInputText(inputText, onCommit);
    }

    protected abstract String getSinglePattern();
    protected abstract boolean isTimeEditor();
    protected abstract boolean isDateEditor();
    protected abstract boolean isSinglePicker();

    private InputElement getInputElement() {
        return inputElement;
    }

    protected String replaceUnsupportedSymbols(String pattern) {
        return pattern.replaceAll("E{3,4} ", "");
    }

    @Override
    protected JavaScriptObject getMaskFromPattern() {
        return SimpleDatePatternConverter.convert(getSinglePattern(), !isSinglePicker());
    }

    protected native void removePicker()/*-{
        this.@DateRangePickerBasedCellEditor::getPickerObject()().remove();
    }-*/;

    protected native void openPicker()/*-{
        this.@DateRangePickerBasedCellEditor::getPickerObject()().toggle();
    }-*/;

    protected native Element getPickerObject()/*-{
        return $(this.@DateRangePickerBasedCellEditor::getInputElement()()).data('daterangepicker');
    }-*/;

    protected native Element getPickerContainer()/*-{
        return this.@DateRangePickerBasedCellEditor::getPickerObject()().container.get(0);
    }-*/;

    protected native void createPicker(Element parent, JsDate startDate, JsDate endDate, String pattern,
                                       boolean singleDatePicker, boolean time, boolean date, boolean autoUpdateInput)/*-{
        window.$ = $wnd.jQuery;
        var thisObj = this;
        var editElement = $(thisObj.@DateRangePickerBasedCellEditor::getInputElement()());
        var messages = @lsfusion.gwt.client.ClientMessages.Instance::get()();

        var options = {
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
                format: $wnd.moment().toMomentFormatString(pattern)
            },
            timePicker: !date,
            timePicker24Hour: true,
            showDropdowns: true,
            autoApply: true,
            ranges: !time && !@lsfusion.gwt.client.view.MainFrame::mobile ? $wnd[singleDatePicker ? 'getSingleRanges' : 'getRanges']($wnd, messages.@lsfusion.gwt.client.ClientMessages::today()(),
                messages.@lsfusion.gwt.client.ClientMessages::yesterday()(),
                singleDatePicker ? messages.@lsfusion.gwt.client.ClientMessages::sevenDaysAgo()() : messages.@lsfusion.gwt.client.ClientMessages::last7Days()(),
                singleDatePicker ? messages.@lsfusion.gwt.client.ClientMessages::thirtyDaysAgo()() : messages.@lsfusion.gwt.client.ClientMessages::last30Days()(),
                singleDatePicker ? messages.@lsfusion.gwt.client.ClientMessages::monthStart()() : messages.@lsfusion.gwt.client.ClientMessages::thisMonth()(),
                singleDatePicker ? messages.@lsfusion.gwt.client.ClientMessages::monthEnd()() : messages.@lsfusion.gwt.client.ClientMessages::toMonthEnd()(),
                singleDatePicker ? messages.@lsfusion.gwt.client.ClientMessages::previousMonthStart()() : messages.@lsfusion.gwt.client.ClientMessages::previousMonth()(),
                singleDatePicker ? messages.@lsfusion.gwt.client.ClientMessages::previousMonthEnd()() : messages.@lsfusion.gwt.client.ClientMessages::monthStartToCurrentDate()(),
                singleDatePicker ? messages.@lsfusion.gwt.client.ClientMessages::thisYearStart()() : messages.@lsfusion.gwt.client.ClientMessages::thisYear()(),
                singleDatePicker ? messages.@lsfusion.gwt.client.ClientMessages::thisYearEnd()() : messages.@lsfusion.gwt.client.ClientMessages::toYearEnd()(),
                messages.@lsfusion.gwt.client.ClientMessages::clear()(), @lsfusion.gwt.client.view.MainFrame::preDefinedDateRangesNames) : undefined,
            singleDatePicker: singleDatePicker,
            drops: 'auto',

//          to make the behaviour when editing a cell with date and a cell with text the same,
//          disable autoUpdateInput so that the field is not filled in automatically when editing starts
            autoUpdateInput: autoUpdateInput,
            opens: 'left', // thisObj.@DateRangePickerBasedCellEditor::getHorzTextAlignment()().@com.google.gwt.dom.client.Style.TextAlign::getCssName()()
            alwaysShowCalendars: true
        };
        if(startDate != null) { // needed for the 2-year digit dates
            options.startDate = startDate;
            options.endDate = endDate;
        }
        editElement.daterangepicker(options);

        if (autoUpdateInput)
            editElement.select();
        else
            thisObj.@DateRangePickerBasedCellEditor::getPickerObject()().autoUpdateInput = true; // return autoUpdateInput after opening the picker and starting editing

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

        editElement.on('cancel.daterangepicker', function () {
            thisObj.@DateRangePickerBasedCellEditor::pickerCancel(*)();
        });

        editElement.on('apply.daterangepicker', function () {
            thisObj.@DateRangePickerBasedCellEditor::pickerApply(*)(parent);
        });
    }-*/;

    // this all needed only for handling 2-year digit dates
    protected abstract JsDate getStartDate(PValue oldValue);
    protected abstract JsDate getEndDate(PValue oldValue);
    protected abstract PValue getValue(JsDate startDate, JsDate endDate);
    private native JsDate getPickerStartDate()/*-{
        var pickerDate = this.@DateRangePickerBasedCellEditor::getPickerObject()().startDate;
        return pickerDate != null && pickerDate.isValid() ? pickerDate.toDate() : null; // toDate because it is "Moment js" object
    }-*/;
    private native JsDate getPickerEndDate()/*-{
        var pickerDate = this.@DateRangePickerBasedCellEditor::getPickerObject()().endDate;
        return pickerDate != null && pickerDate.isValid() ? pickerDate.toDate() : null; // toDate because it is "Moment js" object
    }-*/;

    public static native void setPickerTwoDigitYearStart(Integer twoDigitYearStart)/*-{
        if (twoDigitYearStart != null) {
            $wnd.moment.parseTwoDigitYear = function (yearString) {
                return parseInt(yearString) + (parseInt(yearString) > twoDigitYearStart ? 1900 : 2000);
            }
        }
    }-*/;
}
