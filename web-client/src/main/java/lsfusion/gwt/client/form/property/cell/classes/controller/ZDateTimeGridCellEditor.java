package lsfusion.gwt.client.form.property.cell.classes.controller;

import lsfusion.gwt.client.classes.data.GZDateTimeType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.GZDateTimeDTO;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import java.text.ParseException;
import java.util.Date;

public class ZDateTimeGridCellEditor extends DateTimeGridCellEditor {

    public ZDateTimeGridCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
    }

    @Override
    protected Date valueAsDate(Object value) {
        if (value == null) {
            return null;
        }
        return ((GZDateTimeDTO) value).toDateTime();
    }

    protected Object parseString(String value) throws ParseException {
        return GZDateTimeType.instance.parseString(value, property.pattern);
    }
}