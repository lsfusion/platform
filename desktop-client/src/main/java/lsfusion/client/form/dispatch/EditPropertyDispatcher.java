package lsfusion.client.form.dispatch;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import lsfusion.client.SwingUtils;
import lsfusion.client.form.ClientFormController;
import lsfusion.client.form.EditPropertyHandler;
import lsfusion.client.logics.ClientGroupObjectValue;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.client.logics.classes.ClientType;
import lsfusion.interop.action.UpdateEditValueClientAction;
import lsfusion.interop.action.EditNotPerformedClientAction;
import lsfusion.interop.action.RequestUserInputClientAction;
import lsfusion.interop.form.ServerResponse;
import lsfusion.interop.form.UserInputResult;

import javax.swing.*;
import java.io.IOException;

import static lsfusion.base.BaseUtils.deserializeObject;
import static lsfusion.client.logics.classes.ClientTypeSerializer.deserializeClientType;

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

        super.dispatchResponse(response);
        if (readType != null) {
            ClientType editType = readType;
            readType = null;
            if (!internalRequestValue(editType)) {
                cancelEdit();
            }
            return true;
        }

        return response.resumeInvocation || editPerformed;
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
                    //только в этом случае можно асинхронно посланное значение использовать в качестве текущего
                    boolean canUseNewValueForRendering = simpleChangeProperty.changeType.getTypeClass() == simpleChangeProperty.baseType.getTypeClass();
                    if (canUseNewValueForRendering) {
                        handler.updateEditValue(inputResult.getValue());
                    }
                    getFormController().changeProperty(simpleChangeProperty, editColumnKey, inputResult.getValue(), oldValue, canUseNewValueForRendering);
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
    public void execute(UpdateEditValueClientAction action) {
        try {
            handler.updateEditValue(deserializeObject(action.value));
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }
}
