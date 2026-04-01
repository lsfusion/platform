package lsfusion.gwt.client.form.property.cell.classes.controller;

import lsfusion.gwt.client.classes.data.GADateType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

public class DateTimeCellEditor extends DateCellEditor {

    public DateTimeCellEditor(GADateType type, EditManager editManager, GPropertyDraw property, EditContext editContext) {
        super(type, editManager, property, editContext);
    }

    protected boolean isDateEditor() {
        return false;
    }

    @Override
    protected boolean isTimeEditor() {
        return false;
    }
}
