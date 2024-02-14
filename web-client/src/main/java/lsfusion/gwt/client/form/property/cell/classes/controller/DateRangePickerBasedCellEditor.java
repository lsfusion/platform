package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JsDate;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RendererType;

import java.text.ParseException;

public abstract class DateRangePickerBasedCellEditor extends TextBasedCellEditor implements FormatCellEditor {

    protected boolean isNative() {
        return inputElementType.hasNativePopup();
    }

    public DateRangePickerBasedCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
    }

    @Override
    protected void onInputReady(Element parent, PValue oldValue) {
        super.onInputReady(parent, oldValue);

        if (!isNative()) {
            assert oldValue != null;
            createPicker(GwtClientUtils.getTippyParent(parent), parent, getStartDate(oldValue), getEndDate(oldValue), getPattern().replace("a", "A"), isSinglePicker(), isTimeEditor(), isDateEditor());

            DataGrid.addFocusPartner(parent, getPickerContainer(), element -> element.getPropertyObject("rendering") != null);

            if (needReplace(parent))
                getInputElement().click(); // need to dateRangePicker opens immediately. because we use an editBox
        }
    }

    @Override
    public void stop(Element parent, boolean cancel, boolean blurred) {
        if(!isNative())
            removePicker();
        super.stop(parent, cancel, blurred);
    }

    protected void pickerApply(Element parent) {
        if (editManager.isThisCellEditing(this))
            commit(parent, CommitReason.FORCED);

        if (!needReplace(parent))
            getInputElement().focus();
    }

    protected PValue tryParseInputText(String inputText, boolean onCommit) throws ParseException {
        //to be able to enter the date from keyboard
        if (onCommit) {
            try {
                return super.tryParseInputText(inputText, true);
            } catch (ParseException e) {
                if (isNative())
                    throw e;
                else
                    return getDateInputValue();
            }
        }

        return PValue.getPValue(inputText);
    }

    protected abstract JsDate getStartDate(PValue oldValue);
    protected abstract JsDate getEndDate(PValue oldValue);
    protected abstract String getPattern();
    protected abstract boolean isTimeEditor();
    protected abstract boolean isDateEditor();
    protected abstract PValue getDateInputValue();
    protected abstract boolean isSinglePicker();

    private void setInputValue() {
        setTextInputValue(tryFormatInputText(getDateInputValue()));
    }

    private InputElement getInputElement() {
        return inputElement;
    }

    protected native void removePicker()/*-{
        this.@DateRangePickerBasedCellEditor::getPickerObject()().remove();
        //we need to remove the keydown listener because it is a global($wnd) listener that is only used when the picker popup opens
        $($wnd).off('keydown.pickerpopup').off('mousedown.pickerpopup');
    }-*/;

    protected native Element getPickerObject()/*-{
        return $(this.@DateRangePickerBasedCellEditor::getInputElement()()).data('daterangepicker');
    }-*/;

    protected native Element getPickerContainer()/*-{
        return this.@DateRangePickerBasedCellEditor::getPickerObject()().container.get(0);
    }-*/;

    protected native JsDate getPickerStartDate()/*-{
        var pickerDate = this.@DateRangePickerBasedCellEditor::getPickerObject()().startDate;
        return pickerDate.isValid() ? pickerDate.toDate() : null; // toDate because it is "Moment js" object
    }-*/;

    protected native JsDate getPickerEndDate()/*-{
        var pickerDate = this.@DateRangePickerBasedCellEditor::getPickerObject()().endDate;
        // pickerDate may be null because we update the input field and on select 'date_from' - 'date_to' will be null
        return pickerDate == null ? this.@DateRangePickerBasedCellEditor::getPickerStartDate(*)() : pickerDate.isValid() ? pickerDate.toDate() : null; // toDate because it is "Moment js" object
    }-*/;

    private Style.TextAlign getHorzTextAlignment() {
        return property.getHorzTextAlignment(RendererType.CELL); // should be taken from RenderContext, but for now this would do
    }

    protected native void createPicker(Element tippyParent, Element parent, JsDate startDate, JsDate endDate, String pattern, boolean singleDatePicker, boolean time, boolean date)/*-{
        window.$ = $wnd.jQuery;
        var thisObj = this;
        var editElement = $(thisObj.@DateRangePickerBasedCellEditor::getInputElement()());
        var messages = @lsfusion.gwt.client.ClientMessages.Instance::get()();

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
                format: $wnd.moment().toMomentFormatString(pattern)
            },
            parentEl: tippyParent,
            startDate: startDate,
            endDate: endDate,
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
            opens: thisObj.@DateRangePickerBasedCellEditor::getHorzTextAlignment()().@com.google.gwt.dom.client.Style.TextAlign::getCssName()(),
            alwaysShowCalendars: true, // need to use with ranges
            onKeydown: function (e) {
                if (e.keyCode === 27)
                    thisObj.@ARequestValueCellEditor::cancel(Llsfusion/gwt/client/form/property/cell/controller/CancelReason;)(@lsfusion.gwt.client.form.property.cell.controller.CancelReason::ESCAPE_PRESSED);
            },
            onClickDate: function () {
                thisObj.@DateRangePickerBasedCellEditor::setInputValue()();
            }
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

        //update input element
        $(thisObj.@DateRangePickerBasedCellEditor::getPickerContainer()()).on('keyup change.daterangepicker', function (e) {
            if (e.target.tagName !== 'SELECT')
                thisObj.@DateRangePickerBasedCellEditor::setInputValue()();
        });

        editElement.on('cancel.daterangepicker', function () {
            thisObj.@lsfusion.gwt.client.form.property.cell.classes.controller.DateRangePickerBasedCellEditor::cancel()();
        });

        editElement.on('apply.daterangepicker', function () {
            thisObj.@lsfusion.gwt.client.form.property.cell.classes.controller.DateRangePickerBasedCellEditor::pickerApply(*)(parent);
        });
    }-*/;
}
