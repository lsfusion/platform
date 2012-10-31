package platform.gwt.form.shared.view.grid.editor;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.DatePicker;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.base.shared.GwtSharedUtils;

import java.util.Date;

public class DateGridEditor extends PopupBasedGridEditor {

    private final DateTimeFormat format;
    private DatePicker datePicker;

    public DateGridEditor(EditManager editManager) {
        this(GwtSharedUtils.getDefaultDateFormat(), editManager);
    }

    public DateGridEditor(DateTimeFormat format, EditManager editManager) {
        super(editManager);
        this.format = format;

        datePicker.addValueChangeHandler(new ValueChangeHandler<Date>() {
            @Override
            public void onValueChange(ValueChangeEvent<Date> event) {
                commitEditing(event.getValue());
            }
        });
    }


    @Override
    protected Widget createPopupComponent() {
        return datePicker = new DatePicker();
    }

    @Override
    public void startEditing(NativeEvent editEvent, Cell.Context context, Element parent, Object oldValue) {
        if (oldValue != null) {
            datePicker.setValue((Date) oldValue);
            datePicker.setCurrentMonth((Date) oldValue);
        }
        super.startEditing(editEvent, context, parent, oldValue);
    }

    @Override
    protected String formatValue(Object value) {
        return format.format((Date) value);
    }
}
