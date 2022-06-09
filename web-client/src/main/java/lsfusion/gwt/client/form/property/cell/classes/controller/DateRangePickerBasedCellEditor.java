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

    private void setInputValue(Element parent) {
        setInputValue(getInputElement(parent), tryFormatInputText(getInputValue()));
    }

    protected native void removePicker()/*-{
        $(this.@TextBasedPopupCellEditor::editBox).data('daterangepicker').remove();
        //we need to remove the keydown listener because it is a global($wnd) listener that is only used when the picker popup opens
        $($wnd).off('keydown.pickerpopup').off('click.pickerpopup');
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
        // pickerDate may be null because we update the input field and on select 'date_from' - 'date_to' will be null
        return pickerDate == null ? this.@DateRangePickerBasedCellEditor::getPickerStartDate(*)() : pickerDate.isValid() ? pickerDate.toDate() : null; // toDate because it is "Moment js" object
    }-*/;

    protected native void createPicker(Element parent, JsDate startDate, JsDate endDate, String pattern, boolean singleDatePicker, boolean time, boolean date)/*-{
        window.$ = $wnd.jQuery;
        var thisObj = this;
        var editElement = $(thisObj.@TextBasedPopupCellEditor::editBox);
        var messages = @lsfusion.gwt.client.ClientMessages.Instance::get()();
        applyDateRangePickerPatches(); //Must be called before the picker is initialised

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
                format: pattern.replaceAll("d", "D").replaceAll("y", "Y").replaceAll("a", "A") // dateRangePicker format - date uses capital letters, time uses small letters, AM/PM uses capital letter. need to be able to enter date from keyboard
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
            if (e.target.tagName !== 'SELECT' || e.type !== 'mouseup')
                thisObj.@DateRangePickerBasedCellEditor::setInputValue(Lcom/google/gwt/dom/client/Element;)(parent);
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
            }).on('click.pickerpopup', function (e) { //daterangepicker for some reason returns focus to $wnd and not to the editelement and the daterangepicker does not hide even if we open another form
                var container = $(".daterangepicker");
                if (!container.is(e.target) && container.has(e.target).length === 0)
                    thisObj.@lsfusion.gwt.client.form.property.cell.classes.controller.DateRangePickerBasedCellEditor::pickerApply(*)(parent);
            });
        });

        editElement.on('hide.daterangepicker', function () {
            $($wnd).off('keydown.pickerpopup').off('click.pickerpopup');
        });

        //THESE ARE THE PATCHES OF DATERANGEPICKER. ALL CHANGES START WITH <<<<< AND END WITH >>>>>. ALL COMMENTS WILL BE IN UPPER CASE
        function applyDateRangePickerPatches() {
            //OVERRIDE OF THE datepicker.keydown METHOD. COPIED FROM daterangepicker.js WITH SOME CHANGES
            $wnd.daterangepicker.prototype.keydown = function (e) {
                //<<<<< REMOVE THIS IF TO PREVENT HIDE PICKER ON PRESS ENTER WITH INVALID INPUT
                //hide on tab or enter
                //if ((e.keyCode === 9) || (e.keyCode === 13)) {
                //    this.hide();
                //}
                //>>>>>

                //hide on esc and prevent propagation
                if (e.keyCode === 27) {
                    e.preventDefault();
                    e.stopPropagation();

                    //<<<<< CHANGED hide() TO cancel()
                    //this.hide();
                    thisObj.@ARequestValueCellEditor::cancel(Lcom/google/gwt/dom/client/Element;Llsfusion/gwt/client/form/property/cell/controller/CancelReason;)(parent, @lsfusion.gwt.client.form.property.cell.controller.CancelReason::ESCAPE_PRESSED);
                    //>>>>>
                }
            }

            //OVERRIDE OF THE calculateChosenLabel AND updateCalendars METHODS. COPIED FROM daterangepicker.js.
            //NEED FOR HIGHLIGHTING PREDEFINED RANGES WHEN SHOWN ONLY ONE DATEPICKER NOT INTERVAL.
            $wnd.daterangepicker.prototype.calculateChosenLabel = function () {
                var customRange = true;
                var i = 0;
                for (var range in this.ranges) {
                    //<<<<<
                    //ADDED THIS IF
                    if (singleDatePicker) {
                        if (this.startDate.format('YYYY-MM-DD') == this.ranges[range][0].format('YYYY-MM-DD')) {
                            customRange = false;
                            this.chosenLabel = this.container.find('.ranges li:eq(' + i + ')').addClass('active').attr('data-range-key');
                            break;
                        }
                    }
                    //>>>>>
                    else if (this.timePicker) {
                        var format = this.timePickerSeconds ? "YYYY-MM-DD HH:mm:ss" : "YYYY-MM-DD HH:mm";
                        //ignore times when comparing dates if time picker seconds is not enabled
                        if (this.startDate.format(format) == this.ranges[range][0].format(format) && this.endDate.format(format) == this.ranges[range][1].format(format)) {
                            customRange = false;
                            this.chosenLabel = this.container.find('.ranges li:eq(' + i + ')').addClass('active').attr('data-range-key');
                            break;
                        }
                    } else {
                        //ignore times when comparing dates if time picker is not enabled
                        if (this.startDate.format('YYYY-MM-DD') == this.ranges[range][0].format('YYYY-MM-DD') && this.endDate.format('YYYY-MM-DD') == this.ranges[range][1].format('YYYY-MM-DD')) {
                            customRange = false;
                            this.chosenLabel = this.container.find('.ranges li:eq(' + i + ')').addClass('active').attr('data-range-key');
                            break;
                        }
                    }
                    i++;
                }
                if (customRange) {
                    if (this.showCustomRangeLabel) {
                        this.chosenLabel = this.container.find('.ranges li:last').addClass('active').attr('data-range-key');
                    } else {
                        this.chosenLabel = null;
                    }
                    this.showCalendars();
                }
            }

            $wnd.daterangepicker.prototype.updateCalendars = function() {
                if (this.timePicker) {
                    var hour, minute, second;
                    if (this.endDate) {
                        hour = parseInt(this.container.find('.left .hourselect').val(), 10);
                        minute = parseInt(this.container.find('.left .minuteselect').val(), 10);
                        if (isNaN(minute)) {
                            minute = parseInt(this.container.find('.left .minuteselect option:last').val(), 10);
                        }
                        second = this.timePickerSeconds ? parseInt(this.container.find('.left .secondselect').val(), 10) : 0;
                        if (!this.timePicker24Hour) {
                            var ampm = this.container.find('.left .ampmselect').val();
                            if (ampm === 'PM' && hour < 12)
                                hour += 12;
                            if (ampm === 'AM' && hour === 12)
                                hour = 0;
                        }
                    } else {
                        hour = parseInt(this.container.find('.right .hourselect').val(), 10);
                        minute = parseInt(this.container.find('.right .minuteselect').val(), 10);
                        if (isNaN(minute)) {
                            minute = parseInt(this.container.find('.right .minuteselect option:last').val(), 10);
                        }
                        second = this.timePickerSeconds ? parseInt(this.container.find('.right .secondselect').val(), 10) : 0;
                        if (!this.timePicker24Hour) {
                            var ampm = this.container.find('.right .ampmselect').val();
                            if (ampm === 'PM' && hour < 12)
                                hour += 12;
                            if (ampm === 'AM' && hour === 12)
                                hour = 0;
                        }
                    }
                    this.leftCalendar.month.hour(hour).minute(minute).second(second);
                    this.rightCalendar.month.hour(hour).minute(minute).second(second);
                }

                this.renderCalendar('left');
                this.renderCalendar('right');

                //highlight any predefined range matching the current start and end dates
                this.container.find('.ranges li').removeClass('active');

                //<<<<<
                //CHANGE IF STATEMENT. ADDED !singleDatePicker CHECK
                // if (this.endDate == null) return;
                if (!singleDatePicker && this.endDate == null) return;
                //>>>>>

                this.calculateChosenLabel();
            }

            //OVERRIDE OF THE show METHOD. COPIED FROM daterangepicker.js.
            //WE NEED TO TRIGGER 'show.daterangepicker' BEFORE CONTAINER SHOWN FOR DETERMINATE SIZE OF POPUP
            $wnd.daterangepicker.prototype.show = function(e) {
                if (this.isShowing) return;

                // Create a click proxy that is private to this instance of datepicker, for unbinding
                this._outsideClickProxy = $.proxy(function(e) { this.outsideClick(e); }, this);

                // Bind global datepicker mousedown for hiding and
                $(document)
                    .on('mousedown.daterangepicker', this._outsideClickProxy)
                    // also support mobile devices
                    .on('touchend.daterangepicker', this._outsideClickProxy)
                    // also explicitly play nice with Bootstrap dropdowns, which stopPropagation when clicking them
                    .on('click.daterangepicker', '[data-toggle=dropdown]', this._outsideClickProxy)
                    // and also close when focus changes to outside the picker (eg. tabbing between controls)
                    .on('focusin.daterangepicker', this._outsideClickProxy);

                // Reposition the picker if the window is resized while it's open
                $(window).on('resize.daterangepicker', $.proxy(function(e) { this.move(e); }, this));

                this.oldStartDate = this.startDate.clone();
                this.oldEndDate = this.endDate.clone();
                this.previousRightTime = this.endDate.clone();

                this.updateView();
                //<<<<<
                //ADD
                this.element.trigger('show.daterangepicker', this); //MOVED FROM AFTER this.move();
                this.container.show();
                this.move();
//                this.element.trigger('show.daterangepicker', this);
                //>>>>>
                this.isShowing = true;
            }
        }
    }-*/;
}
