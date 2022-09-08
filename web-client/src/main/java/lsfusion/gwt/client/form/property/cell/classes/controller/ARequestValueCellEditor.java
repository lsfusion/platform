package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.controller.SmartScheduler;
import lsfusion.gwt.client.form.property.cell.controller.CancelReason;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;

// editor that requests value
public abstract class ARequestValueCellEditor implements RequestValueCellEditor {

    protected final EditManager editManager;

    public ARequestValueCellEditor(EditManager editManager) {
        this.editManager = editManager;
    }

    // force commit with the current value
    public void commit(Element parent, CommitReason commitReason) {
        validateAndCommit(parent, null, commitReason != CommitReason.ENTERPRESSED, commitReason);
    }

    // force commit with the specified value
    public void commitValue(Element parent, Object value) {
        commitFinish(parent, value, null, CommitReason.FORCED);
    }

    // force cancel
    public void cancel(Element parent, CancelReason cancelReason) {
        editManager.cancelEditing(cancelReason);
    }

    private boolean deferredCommitOnBlur = false;

    //some libraries set values after the blur. to solve this there is a SmartScheduler that sets the values in the field before the blur
    protected boolean isDeferredCommitOnBlur() {
        return deferredCommitOnBlur;
    }

    public void setDeferredCommitOnBlur(boolean deferredCommitOnBlur) {
        this.deferredCommitOnBlur = deferredCommitOnBlur;
    }

    protected void commitFinish(Element parent, Object value, Integer contextAction, CommitReason commitReason) {
        editManager.commitEditing(new GUserInputResult(value, contextAction), commitReason);
    }

    protected boolean isThisCellEditor() {
        return editManager.isThisCellEditing(this);
    }

    public void validateAndCommit(Element parent, boolean cancelIfInvalid, CommitReason commitReason) {
        validateAndCommit(parent, null, cancelIfInvalid, commitReason);
    }

    public void validateAndCommit(Element parent, Integer contextAction, boolean cancelIfInvalid, CommitReason commitReason) {
        SmartScheduler.getInstance().scheduleDeferred(commitReason.isBlurred() && isDeferredCommitOnBlur(), () -> {
            if (editManager.isThisCellEditing(this)) {
                Object value = getValue(parent, contextAction);
                if (value == null || !value.equals(RequestValueCellEditor.invalid))
                    commitFinish(parent, value, contextAction, commitReason);
                else if (cancelIfInvalid)
                    cancel(parent);
            }
        });
    }
}
