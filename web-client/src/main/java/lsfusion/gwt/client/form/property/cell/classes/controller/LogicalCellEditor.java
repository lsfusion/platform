package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.KeepCellEditor;

public class LogicalCellEditor implements KeepCellEditor {
    public LogicalCellEditor(EditManager editManager) {
        this.editManager = editManager;
    }

    protected EditManager editManager;

    @Override
    public void startEditing(Event editEvent, Element parent, Object oldValue) {
        Boolean currentValue = (Boolean) oldValue;
        editManager.commitEditing(currentValue == null || !currentValue ? true : null);
    }
}
