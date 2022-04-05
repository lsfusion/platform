package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.KeepCellEditor;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;

import static lsfusion.gwt.client.base.view.grid.AbstractDataGridBuilder.*;

public class LogicalCellEditor implements KeepCellEditor {

    private boolean threeState;

    public LogicalCellEditor(EditManager editManager, boolean threeState) {
        this.editManager = editManager;
        this.threeState = threeState;
    }

    protected EditManager editManager;

    @Override
    public void start(Event editEvent, Element parent, Object oldValue) {
        parent.setAttribute(IGNORE_DBLCLICK_AFTER_CLICK, "true");

        new Timer() {
            @Override
            public void run() {
                parent.removeAttribute(IGNORE_DBLCLICK_AFTER_CLICK);
            }
        }.schedule(500);

        Boolean nextValue = getNextValue(oldValue, threeState);
        Object value = threeState ? nextValue : (nextValue != null && nextValue ? true : null);
        editManager.commitEditing(new GUserInputResult(value), CommitReason.OTHER);
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
