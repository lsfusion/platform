package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.event.GMouseStroke;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.classes.view.InputBasedCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.KeepCellEditor;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.view.MainFrame;

public class LogicalCellEditor extends ARequestValueCellEditor implements KeepCellEditor {

    private boolean threeState;

    public LogicalCellEditor(EditManager editManager, boolean threeState) {
        super(editManager);
        this.threeState = threeState;
    }

    @Override
    public void start(EventHandler handler, Element parent, RenderContext renderContext, boolean notFocusable, PValue oldValue) {
        MainFrame.preventDblClickAfterClick(parent);

        value = getNextValue(oldValue, threeState);

        // there are two ways to make checkbox readonly (and we need this, since we use "different" events as a change events)
        // pointer-events:none, but in that case mouse events won't work without a wrapper
        // commit changes after change in the control is done (CHANGE event) that
        if(!( // it's important to delay only that events that will lead to the CHANGE event (otherwise there will be no CHANGE event to wait)
            InputBasedCellRenderer.getInputEventTarget(parent, handler.event) != null &&
            (GMouseStroke.isChangeEvent(handler.event)
            || GKeyStroke.isLogicalInputChangeEvent(handler.event)))) {
            assert InputBasedCellRenderer.getInputElementType(InputBasedCellRenderer.getInputElement(parent)).isLogical();
            commit(parent);
        }
    }

    @Override
    public void onBrowserEvent(Element parent, EventHandler handler) {
        if(GKeyStroke.isChangeEvent(handler.event)) {
            commit(parent);
            handler.consume();
        }
    }

    private PValue value;
    @Override
    public PValue getCommitValue(Element parent, Integer contextAction) {
        return value;
    }

    @Override
    public void stop(Element parent, boolean cancel, boolean blurred) {
        value = null;
    }

    private PValue getNextValue(PValue value, boolean threeState) {
        if (threeState) {
            Boolean value3s = PValue.get3SBooleanValue(value);
            Boolean nextValue3s;
            if (value3s == null)
                nextValue3s = true;
            else if (value3s)
                nextValue3s = false;
            else
                nextValue3s = null;
            return PValue.getPValue(nextValue3s);
        } else {
            return PValue.getPValue(!PValue.getBooleanValue(value));
        }
    }
}
