package platform.client.form.dispatch;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import platform.client.SwingUtils;
import platform.client.form.ClientFormController;
import platform.client.form.EditPropertyHandler;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.client.logics.classes.ClientType;
import platform.interop.action.AsyncResultClientAction;
import platform.interop.action.EditNotPerformedClientAction;
import platform.interop.action.RequestUserInputClientAction;
import platform.interop.form.ServerResponse;
import platform.interop.form.UserInputResult;

import javax.swing.*;
import java.io.IOException;

import static platform.base.BaseUtils.deserializeObject;
import static platform.client.logics.classes.ClientTypeSerializer.deserializeClientType;

public class EditPropertyDispatcher extends ClientFormActionDispatcher {
    protected final EditPropertyHandler handler;

    private boolean valueRequested = false;
    private boolean editPerformed;

    private ClientGroupObjectValue editColumnKey;
    private ClientPropertyDraw simpleChangeProperty;

    private ClientType readType;
    private Object oldValue;

    public EditPropertyDispatcher(EditPropertyHandler handler) {
        this.handler = handler;
    }

    @Override
    public ClientFormController getFormController() {
        return handler.getForm();
    }

    /**
     * @return true, если на сервере вызван action для редактирования
     */
    public boolean executePropertyEditAction(ClientPropertyDraw property, ClientGroupObjectValue columnKey, String actionSID, Object currentValue) {
        try {
            readType = null;
            simpleChangeProperty = null;
            editColumnKey = null;

            ClientFormController form = getFormController();

            if (actionSID.equals(ServerResponse.CHANGE)) { // асинхронные обработки
                boolean asyncModifyObject = form.isAsyncModifyObject(property);
                if (asyncModifyObject || property.changeType != null) {
                    if (property.askConfirm) {
                        String msg = property.askConfirmMessage;

                        int result = SwingUtils.showConfirmDialog(getDialogParentContainer(), msg, "lsFusion", JOptionPane.QUESTION_MESSAGE);
                        if (result != JOptionPane.YES_OPTION) {
                            return true;
                        }
                    }

                    if (asyncModifyObject) {
                        form.modifyObject(property, columnKey);
                        return true;
                    } else {
//                      т.е. property.changeType != null
                        editColumnKey = columnKey;
                        simpleChangeProperty = property;
                        oldValue = currentValue;
                        return internalRequestValue(property.changeType);
                    }
                }
            }

            editPerformed = true;
            ServerResponse response = form.executeEditAction(property, columnKey, actionSID);
            return internalDispatchResponse(response);
        } catch (IOException ex) {
            throw Throwables.propagate(ex);
        }
    }

    @Override
    public void dispatchResponse(ServerResponse response) throws IOException {
        internalDispatchResponse(response);
    }

    /**
     * @return true, если на сервере вызван action для редактирования
     */
    private boolean internalDispatchResponse(ServerResponse response) throws IOException {
        assert response != null;

        ClientType editType = null;
        super.dispatchResponse(response);
        if (readType != null) {
            editType = readType;
            readType = null;
            if (!internalRequestValue(editType)) {
                cancelEdit();
            }
            return true;
        }

        return editType != null || response.resumeInvocation || editPerformed;
    }

    private boolean internalRequestValue(ClientType readType) throws IOException {
        valueRequested = true;
        return handler.requestValue(readType, oldValue);
    }

    private void internalCommitValue(UserInputResult inputResult) {
        Preconditions.checkState(valueRequested, "value wasn't requested");

        if (simpleChangeProperty != null) {
            if (!inputResult.isCanceled()) {
                try {
                    getFormController().changeProperty(handler, simpleChangeProperty, editColumnKey, inputResult.getValue(), oldValue);
                } catch (IOException e) {
                    throw Throwables.propagate(e);
                }
            }
            return;
        }

        valueRequested = false;
        continueDispatching(inputResult);
    }

    public void commitValue(Object value) {
        internalCommitValue(new UserInputResult(value));
    }

    public void cancelEdit() {
        internalCommitValue(UserInputResult.canceled);
    }

    public Object execute(RequestUserInputClientAction action) {
        try {
            readType = deserializeClientType(action.readType);
            oldValue = deserializeObject(action.oldValue);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        pauseDispatching();

        return null;
    }

    @Override
    public void execute(EditNotPerformedClientAction action) {
        editPerformed = false;
    }

    @Override
    public void execute(AsyncResultClientAction action) {
        try {
            handler.updateEditValue(deserializeObject(action.value));
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }
}
