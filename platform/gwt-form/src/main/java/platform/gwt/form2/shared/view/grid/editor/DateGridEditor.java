package platform.gwt.form2.shared.view.grid.editor;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.DatePicker;
import platform.gwt.form2.shared.view.grid.EditManager;
import platform.gwt.utils.GwtSharedUtils;

import java.util.Date;

public class DateGridEditor extends PopupBasedGridEditor {

    private final DateTimeFormat format;
    private DatePicker datePicker;

    public DateGridEditor(EditManager editManager, Object oldValue) {
        this(GwtSharedUtils.getDefaultDateFormat(), editManager, oldValue);
    }

    public DateGridEditor(DateTimeFormat format, EditManager editManager, Object oldValue) {
        super(editManager);
        this.format = format;

        datePicker.addValueChangeHandler(new ValueChangeHandler<Date>() {
            @Override
            public void onValueChange(ValueChangeEvent<Date> event) {
                commitEditing(event.getValue());
            }
        });

        if (oldValue != null) {
            datePicker.setValue((Date) oldValue);
            datePicker.setCurrentMonth((Date) oldValue);
        }
    }


    @Override
    protected Widget createPopupComponent() {
        return datePicker = new DatePicker();
    }

    @Override
    protected String formatValue(Object value) {
        return format.format((Date) value);
    }
}
