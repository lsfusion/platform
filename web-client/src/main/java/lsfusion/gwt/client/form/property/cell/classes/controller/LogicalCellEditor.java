package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.KeepCellEditor;

public class LogicalCellEditor implements KeepCellEditor {

    private boolean threeState;

    public LogicalCellEditor(EditManager editManager, boolean threeState) {
        this.editManager = editManager;
        this.threeState = threeState;
    }

    protected EditManager editManager;

    @Override
    public void startEditing(Event editEvent, Element parent, Object oldValue) {
        editManager.commitEditing(getNextValue(oldValue, threeState));
    }

    private Boolean getNextValue(Object value, boolean threeState) {
        if (threeState) {
            if (value == null) return true;
            if ((boolean) value) return false;
            return null;
        } else {
            return value == null || !(boolean) value;
        }
    }
}
