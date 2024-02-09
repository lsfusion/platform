package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.controller.SmartScheduler;
import lsfusion.gwt.client.form.property.PValue;
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
    public void commitValue(PValue value) {
        commitFinish(value, null, CommitReason.FORCED);
    }

    // force cancel
    public void cancel(CancelReason cancelReason) {
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

    protected void commitFinish(PValue value, Integer contextAction, CommitReason commitReason) {
        editManager.commitEditing(new GUserInputResult(value, contextAction), commitReason);
    }

    protected boolean isThisCellEditor() {
        return editManager.isThisCellEditing(this);
    }

    public void validateAndCommit(Element parent, boolean cancelIfInvalid, CommitReason commitReason) {
        validateAndCommit(parent, null, cancelIfInvalid, commitReason);
    }

    private boolean cancelTheSameValueOnBlur;
    private Object cancelTheSameValueOnBlurOldValue;
    @Override
    public void setCancelTheSameValueOnBlur(Object oldValue) {
        cancelTheSameValueOnBlur = true;
        cancelTheSameValueOnBlurOldValue = oldValue;
    }

    public void validateAndCommit(Element parent, Integer contextAction, boolean cancelIfInvalid, CommitReason commitReason) {
        boolean blurred = commitReason.isBlurred();
        SmartScheduler.getInstance().scheduleDeferred(blurred && isDeferredCommitOnBlur(), () -> {
            if (editManager.isThisCellEditing(this)) {
                try {
                    PValue value = getCommitValue(parent, contextAction);
                    if (cancelTheSameValueOnBlur && (blurred || commitReason.isForcedBlurred()) && GwtClientUtils.nullEquals(value, cancelTheSameValueOnBlurOldValue)) {
                        cancel();
                    } else
                        commitFinish(value, contextAction, commitReason);
                } catch (InvalidEditException e) {
                    if (cancelIfInvalid && !e.patternMismatch)
                        cancel();
                }
            }
        });
    }
}
