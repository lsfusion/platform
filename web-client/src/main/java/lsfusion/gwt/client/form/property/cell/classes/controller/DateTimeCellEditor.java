package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.classes.data.GADateType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.GDateTimeDTO;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import java.util.Date;

public class DateTimeCellEditor extends DateCellEditor {

    public DateTimeCellEditor(GADateType type, EditManager editManager, GPropertyDraw property) {
        super(type, editManager, property);
    }

    @Override
    protected Date preProceedDate(Date date) {
        date.setHours(0);
        date.setMinutes(0);
        date.setSeconds(0);
        return date;
    }
}
