package lsfusion.gwt.client.form.property.cell.classes.controller;

import lsfusion.gwt.client.classes.data.GFormatType;
import lsfusion.gwt.client.classes.data.GTimeType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

public class TimeCellEditor extends DateTimeCellEditor {

    @Override
    public GFormatType getFormatType() {
        return type;
    }

    public TimeCellEditor(GTimeType type, EditManager editManager, GPropertyDraw property) {
        super(type, editManager, property);
    }

    @Override
    protected boolean isDateEditor() {
        return false;
    }

    @Override
    protected boolean isTimeEditor() {
        return true;
    }
}
