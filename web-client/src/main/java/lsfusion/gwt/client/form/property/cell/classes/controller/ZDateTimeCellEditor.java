package lsfusion.gwt.client.form.property.cell.classes.controller;

import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.classes.data.GADateType;
import lsfusion.gwt.client.classes.data.GZDateTimeType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.GZDateTimeDTO;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import java.text.ParseException;
import java.util.Date;

public class ZDateTimeCellEditor extends DateTimeCellEditor {

    public ZDateTimeCellEditor(GADateType type, EditManager editManager, GPropertyDraw property) {
        super(type, editManager, property);
    }
}