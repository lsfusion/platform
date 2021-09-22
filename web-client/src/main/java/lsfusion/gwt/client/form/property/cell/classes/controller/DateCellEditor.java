package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.view.ResizableVerticalPanel;
import lsfusion.gwt.client.classes.data.GDateType;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.GDateDTO;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.view.StyleDefaults;

import java.text.ParseException;
import java.util.Date;

public class DateCellEditor extends PopupBasedCellEditor {

    private static final DateTimeFormat format = GwtSharedUtils.getDefaultDateFormat(true);

    protected GDatePicker datePicker;
    protected TextBox editBox;

    public DateCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
    }

    @Override
    protected Widget createPopupComponent(Element parent, Object oldValue) {
        ResizableVerticalPanel panel = new ResizableVerticalPanel();

        editBox = new TextBox();
        editBox.addStyleName("dateTimeEditorBox");
        editBox.setHeight(StyleDefaults.VALUE_HEIGHT_STRING);
        panel.add(editBox);
        editBox.addKeyPressHandler(new KeyPressHandler() {
            @Override
            public void onKeyPress(KeyPressEvent event) {
                if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
                    onCommitEvent(event, parent);
                }
            }
        });


        datePicker = new GDatePicker();
        panel.add(datePicker);
        datePicker.addValueChangeHandler(event -> onDateChanged(event, parent));
        datePicker.getCalendarView().addDomHandler(event -> {
            EventTarget eventTarget = event.getNativeEvent().getEventTarget();
            if (eventTarget != null) {
                String className = Element.as(eventTarget).getClassName();
                if (className.contains("datePickerDay") && !className.contains("datePickerDayIsFiller")) {
                    DateCellEditor.this.onCommitEvent(event, parent);
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

        if (oldDate != null) {
            datePicker.setValue(oldDate);
            datePicker.setCurrentMonth(oldDate);
        } else {
            datePicker.setValue(new Date());
        }
        editBox.setValue(
                input != null ? input : formatToString(oldDate != null ? oldDate : new Date())
        );

        editBox.getElement().focus();
        if (selectAll) {
            editBox.setSelectionRange(0, editBox.getValue().length());
        } else {
            editBox.setSelectionRange(editBox.getValue().length(), 0);
        }
    }

    protected String formatToString(Date date) {
        return format.format(date);
    }

    protected Date valueAsDate(Object value) {
        if (value instanceof GDateDTO) {
            return ((GDateDTO) value).toDate();
        }
        return null;
    }

    protected void onDateChanged(ValueChangeEvent<Date> event, Element parent) {
        commitValue(parent, GDateDTO.fromDate(event.getValue()));
    }

    protected void onCommitEvent(DomEvent event, Element parent) {
        GwtClientUtils.stopPropagation(event);
        validateAndCommit(parent, false);
    }

    @Override
    public Object getValue(Element parent, Integer contextAction) {
        try {
            return parseString(editBox.getValue());
        } catch (ParseException ignored) {
            return RequestValueCellEditor.invalid;
        }
    }

    protected Object parseString(String value) throws ParseException {
        return GDateType.instance.parseString(value, property.pattern);
    }
    
    public class GDatePicker extends DatePicker {
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
