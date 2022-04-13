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
import lsfusion.gwt.client.classes.data.GDateType;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.GDateDTO;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import java.text.ParseException;
import java.util.Date;

public class DateCellEditor extends TextBasedPopupCellEditor {

    private static final DateTimeFormat format = GwtSharedUtils.getDefaultDateFormat(true);

    protected GDatePicker datePicker;

    public DateCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
    }

    protected Widget createPopupComponent(Element parent) {
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
        return panel;
    }

    @Override
    public void start(Event event, Element parent, Object oldValue) {
        String input = null;
        boolean selectAll = true;
        if (GKeyStroke.isCharDeleteKeyEvent(event)) {
            input = "";
            selectAll = false;
        } else if (GKeyStroke.isCharAddKeyEvent(event)) {
            input = String.valueOf((char) event.getCharCode());
            selectAll = false;
        }

        Date oldDate = valueAsDate(oldValue);

        super.start(event, parent, oldDate);
        GwtClientUtils.showPopupInWindow(popup, createPopupComponent(parent), parent.getAbsoluteLeft(), parent.getAbsoluteBottom());

        if (oldDate != null) {
            datePicker.setValue(oldDate);
            datePicker.setCurrentMonth(oldDate);
        } else {
            datePicker.setValue(new Date());
        }

        popup.addAutoHidePartner(editBox);
        editBox.setValue(
                input != null ? input : formatToString(oldDate != null ? oldDate : new Date())
        );

        editBox.focus();
        if (selectAll)
            editBox.select();
    }

    protected String formatToString(Date date) {
        return format.format(date);
    }

    protected Date valueAsDate(Object value) {
        return value instanceof GDateDTO ? ((GDateDTO) value).toDate() : null;
    }

    protected void onDateChanged(ValueChangeEvent<Date> event, Element parent) {
        editBox.setValue(formatToString(event.getValue()));
    }

    protected Object parseString(String value) throws ParseException {
        return GDateType.instance.parseString(value, property.pattern);
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
