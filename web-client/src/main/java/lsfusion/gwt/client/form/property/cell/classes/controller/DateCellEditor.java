package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.view.ResizableVerticalPanel;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.classes.data.GADateType;
import lsfusion.gwt.client.classes.data.GFormatType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.GDateDTO;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import java.text.ParseException;
import java.util.Date;

public class DateCellEditor extends TextBasedPopupCellEditor implements FormatCellEditor {

    protected GDatePicker datePicker;

    protected GADateType type;

    public DateCellEditor(GADateType type, EditManager editManager, GPropertyDraw property) {
        super(editManager, property);

        this.type = type;
    }

    @Override
    public GFormatType getFormatType() {
        return type;
    }

    protected Widget createPopupComponent(Element parent, Object oldValue) {
        ResizableVerticalPanel panel = new ResizableVerticalPanel();
        datePicker = new GDatePicker();
        panel.add(datePicker);
        datePicker.addValueChangeHandler(event -> onDateChanged(event, parent));
        datePicker.getCalendarView().addDomHandler(event -> {
            EventTarget eventTarget = event.getNativeEvent().getEventTarget();
            if (eventTarget != null) {
                String className = Element.as(eventTarget).getClassName();
                if (className.contains("datePickerDay") && !className.contains("datePickerDayIsFiller")) {
                    GwtClientUtils.stopPropagation(event);
                    validateAndCommit(parent, false, CommitReason.OTHER);
                }
            }
        }, DoubleClickEvent.getType());


        if (oldValue != null) {
            Date oldDate = type.toDate(oldValue);
            datePicker.setValue(oldDate);
            datePicker.setCurrentMonth(oldDate);
        } else {
            datePicker.setValue(new Date());
        }
        return panel;
    }

    protected Date preProceedDate(Date date) {
        return date;
    }

    protected void onDateChanged(ValueChangeEvent<Date> event, Element parent) {
        setInputValue(type.fromDate(preProceedDate(event.getValue())));
        editBox.focus();
    }

    public static class GDatePicker extends DatePicker {
        public GDatePicker() {
            super(new DefaultMonthSelector(), new DefaultCalendarView(), new CalendarModel() {
                protected DateTimeFormat getMonthAndYearFormatter() {
                    return DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.YEAR_MONTH);
                }
            });
        }

        public CalendarView getCalendarView() {
            return getView();
        }
    }
}
