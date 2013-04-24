package platform.gwt.form.shared.view.grid.editor;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.DatePicker;
import platform.gwt.base.client.ui.ResizableVerticalPanel;
import platform.gwt.base.shared.GwtSharedUtils;
import platform.gwt.cellview.client.cell.Cell;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditEvent;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.NativeEditEvent;

import java.util.Date;

import static com.google.gwt.dom.client.BrowserEvents.KEYDOWN;
import static com.google.gwt.dom.client.BrowserEvents.KEYPRESS;

public class DateGridCellEditor extends PopupBasedGridCellEditor {

    private final DateTimeFormat format;
    private DatePicker datePicker;
    private TextBox editBox;

    public DateGridCellEditor(EditManager editManager, GPropertyDraw property) {
        this(GwtSharedUtils.getDefaultDateFormat(), editManager, property);
    }

    public DateGridCellEditor(final DateTimeFormat format, EditManager editManager, GPropertyDraw property) {
        super(editManager, property, Style.TextAlign.RIGHT);
        this.format = format;

        datePicker.addValueChangeHandler(new ValueChangeHandler<Date>() {
            @Override
            public void onValueChange(ValueChangeEvent<Date> event) {
                commitEditing(event.getValue());
            }
        });

        editBox.addKeyPressHandler(new KeyPressHandler() {
            @Override
            public void onKeyPress(KeyPressEvent event) {
                if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
                    try {
                        commitEditing(parseString(editBox.getValue()));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        });
    }

    @Override
    protected Widget createPopupComponent() {
        ResizableVerticalPanel panel = new ResizableVerticalPanel();

        editBox = new TextBox();
        editBox.addStyleName("dateTimeEditorBox");
        panel.add(editBox);

        datePicker = new DatePicker();
        panel.add(datePicker);

        return panel;
    }

    @Override
    public void startEditing(EditEvent editEvent, Cell.Context context, Element parent, Object oldValue) {
        String input = null;
        boolean selectAll = true;
        if (editEvent instanceof NativeEditEvent) {
            NativeEvent nativeEvent = ((NativeEditEvent) editEvent).getNativeEvent();
            String eventType = nativeEvent.getType();
            if (KEYDOWN.equals(eventType) && nativeEvent.getKeyCode() == KeyCodes.KEY_DELETE) {
                input = "";
                selectAll = false;
            } else if (KEYPRESS.equals(eventType)) {
                input = String.valueOf((char)nativeEvent.getCharCode());
                selectAll = false;
            }
        }

        if (oldValue != null) {
            datePicker.setValue((Date) oldValue);
            datePicker.setCurrentMonth((Date) oldValue);
        } else {
            datePicker.setValue(new Date());
        }
        String stringDate = input != null ? input  : (oldValue != null ? renderToString(oldValue) : format.format(new Date()));
        editBox.setValue(stringDate);

        super.startEditing(editEvent, context, parent, oldValue);

        editBox.getElement().focus();
        if (selectAll) {
            editBox.setSelectionRange(0, editBox.getValue().length());
        }
    }

    @Override
    protected String renderToString(Object value) {
        return value == null ? "" : format.format((Date) value);
    }

    private java.sql.Date parseString(String value) {
        if (value.isEmpty()) {
            return null;
        } else {
            Date resultDate;
            if (value.split("\\.").length == 2) {
                resultDate = format.parse(value + "." + (new Date().getYear() - 100));
            } else {
                resultDate = format.parse(value);
            }
            return new java.sql.Date(resultDate.getTime());
        }
    }
}
