package platform.client.form;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.form.EditActionResult;
import platform.interop.form.UserInputResult;

import java.io.IOException;

import static platform.base.BaseUtils.deserializeObject;
import static platform.client.logics.classes.ClientTypeSerializer.deserialize;

public class EditPropertyDispatcher {

    protected final EditPropertyHandler handler;
    private boolean valueRequested = false;

    public EditPropertyDispatcher(EditPropertyHandler handler) {
        this.handler = handler;
    }

    /**
     * @return true, если на сервере вызван action для редактирования
     */
    public boolean executePropertyEditAction(ClientPropertyDraw property, ClientGroupObjectValue columnKey, String actionSID) {
        Preconditions.checkState(!handler.getForm().isBusy(), "There is already server interaction in progress");

        try {
            EditActionResult result = handler.getForm().executeEditAction(property, columnKey, actionSID);
            return dispatchResult(result);
        } catch (IOException ex) {
            throw Throwables.propagate(ex);
        }
    }

    /**
     * @return true, если на сервере вызван action для редактирования
     */
    private boolean dispatchResult(EditActionResult result) throws IOException {
        assert result != null;

        handler.getForm().setBusy(true);

        boolean editPerformed = result.resumeInvocation || result.readType != null || result.actions != null;
        try {
            do {
                Object[] actionResults = handler.getForm().getActionDispatcher().dispatchActions(result.actions);

                if (result.readType != null) {
                    if (!internalRequestValue(result.readType, result.oldValue)) {
                        cancelEdit();
                    }
                    return true;
                }

                if (result.resumeInvocation) {
                    result = handler.getForm().continueExecuteEditAction(actionResults);
                } else {
                    result = null;
                }
            } while (result != null);

            handler.getForm().applyRemoteChanges();
            handler.getForm().setBusy(false);
        } catch(Exception e) {
            handler.getForm().setBusy(false);
            Throwables.propagateIfPossible(e, IOException.class);
        }

        return editPerformed;
    }

    public void commitValue(Object value) {
        internalCommitValue(new UserInputResult(value));
    }

    public void cancelEdit() {
        internalCommitValue(UserInputResult.canceled);
    }

    private boolean internalRequestValue(byte[] readType, byte[] oldValue) throws IOException {
        valueRequested = true;
        return handler.requestValue(deserialize(readType), deserializeObject(oldValue));
    }

    private void internalCommitValue(UserInputResult inputResult) {
        Preconditions.checkState(valueRequested, "value wasn't requested");

        try {
            valueRequested = false;
            dispatchResult(handler.getForm().continueExecuteEditAction(inputResult));
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
