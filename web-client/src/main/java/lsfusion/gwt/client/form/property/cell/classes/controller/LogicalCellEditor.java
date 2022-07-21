package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.event.GMouseStroke;
import lsfusion.gwt.client.form.property.cell.classes.view.LogicalCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.CancelReason;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.KeepCellEditor;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;
import lsfusion.gwt.client.view.MainFrame;

import java.util.Arrays;

public class LogicalCellEditor extends ARequestValueCellEditor implements KeepCellEditor {

    private boolean threeState;

    public LogicalCellEditor(EditManager editManager, boolean threeState) {
        super(editManager);
        this.editManager = editManager;
        this.threeState = threeState;
    }

    protected EditManager editManager;

    @Override
    public void start(EventHandler handler, Element parent, Object oldValue) {
        MainFrame.preventDblClickAfterClick(parent);

        Boolean nextValue = getNextValue(oldValue, threeState);
        value = threeState ? nextValue : (nextValue != null && nextValue ? true : null);

        // there are two ways to make checkbox readonly (and we need this, since we use "different" events as a change events)
        // pointer-events:none, but in that case mouse events won't work without a wrapper
        // commit changes after change in the control is done (CHANGE event) that
        if(!( // it's important to delay only that events that will lead to the CHANGE event (otherwise there will be no CHANGE event to wait)
            LogicalCellRenderer.getInputElement(parent) == handler.event.getEventTarget().cast() &&
            (GMouseStroke.isChangeEvent(handler.event)
            || GKeyStroke.isLogicalInputChangeEvent(handler.event))))
            commit(parent);
    }

    public void commit(Element parent) {
        commit(parent, CommitReason.FORCED);
    }

    private Object value;
    @Override
    public Object getValue(Element parent, Integer contextAction) {
        return value;
    }

    @Override
    public void stop(Element parent, boolean cancel, boolean blurred) {
        value = null;
    }

    @Override
    public void onBrowserEvent(Element parent, EventHandler handler) {
        if(GKeyStroke.isChangeEvent(handler.event)) {
            commit(parent);
            handler.consume();
        }
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
