package lsfusion.client.form.property.cell.controller.dispatch;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import lsfusion.base.lambda.Callback;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.classes.ClientType;
import lsfusion.client.controller.dispatch.DispatcherListener;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.controller.dispatch.ClientFormActionDispatcher;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.controller.EditPropertyHandler;
import lsfusion.interop.action.EditNotPerformedClientAction;
import lsfusion.interop.action.RequestUserInputClientAction;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.action.UpdateEditValueClientAction;
import lsfusion.interop.form.property.cell.UserInputResult;

import javax.swing.*;
import java.io.IOException;
import java.util.EventObject;

import static lsfusion.base.BaseUtils.deserializeObject;
import static lsfusion.client.classes.ClientTypeSerializer.deserializeClientType;

public class EditPropertyDispatcher extends ClientFormActionDispatcher {
    protected final EditPropertyHandler handler;

    private boolean valueRequested = false;
    private Object oldValueRequested;
    private boolean editPerformed;

    private ClientGroupObjectValue editColumnKey;
    private ClientPropertyDraw simpleChangeProperty;

    private ClientType readType;
    private Object oldValue;
    private boolean hasOldValue;
    private Callback<Object> updateEditValueCallback;

    public EditPropertyDispatcher(EditPropertyHandler handler, DispatcherListener dispatcherListener) {
        super(dispatcherListener);
        this.handler = handler;
    }

    @Override
    public ClientFormController getFormController() {
        return handler.getForm();
    }

    public boolean executePropertyEventAction(ClientPropertyDraw property, ClientGroupObjectValue columnKey, String actionSID, EventObject editEvent) {
        valueRequested = false;
        oldValueRequested = null;
        oldValue = null;
        hasOldValue = false;

        readType = null;
        simpleChangeProperty = null;
        editColumnKey = null;

        setEditEvent(editEvent);

        try {
            ClientFormController form = getFormController();

            if (actionSID.equals(ServerResponse.CHANGE)) { // асинхронные обработки
                boolean asyncModifyObject = form.isAsyncModifyObject(property);
                if (asyncModifyObject || property.changeType != null) {
                    if (property.askConfirm) {
                        String msg = property.askConfirmMessage;

                        int result = SwingUtils.showConfirmDialog(getDialogParentContainer(), msg, "lsFusion", JOptionPane.QUESTION_MESSAGE, false);
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
                        return internalRequestValue(property.changeType);
                    }
                }
                boolean isAsyncOpenForm = form.isAsyncOpenForm(property);
                if (isAsyncOpenForm) {
                    form.asyncOpenForm(property);
                    //return true; //comment
                }
            }

            editPerformed = true;
            ServerResponse response = form.executeEventAction(property, columnKey, actionSID);
            try {
                return internalDispatchResponse(response);
            } finally {
                if(response != ServerResponse.EMPTY) // проверка нужна, если запрос заблокируется то и postponeDispatchingEnded не будет, а значит "скобки" нарушатся и упадет assertion
                    dispatcherListener.dispatchingPostponedEnded(this);
            }
        } catch (IOException ex) {
            throw Throwables.propagate(ex);
        } finally {
            setEditEvent(null);
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
        oldValueRequested = handler.getEditValue();
        return handler.requestValue(readType, hasOldValue ? oldValue : oldValueRequested);
    }

    private void internalCommitValue(UserInputResult inputResult) {
        Preconditions.checkState(valueRequested, "value wasn't requested");

        valueRequested = false;
        Object oldValueRequested = this.oldValueRequested;
        this.oldValueRequested = null;

        if (simpleChangeProperty != null) {
            if (!inputResult.isCanceled()) {
                try {
                    //только в этом случае можно асинхронно посланное значение использовать в качестве текущего
                    if (simpleChangeProperty.canUseChangeValueForRendering()) {
                        handler.updateEditValue(inputResult.getValue());
                        if (updateEditValueCallback != null) {
                            updateEditValueCallback.done(inputResult.getValue());
                        }
                    }
                    getFormController().changeProperty(simpleChangeProperty, editColumnKey, inputResult.getValue(), oldValueRequested);
                } catch (IOException e) {
                    throw Throwables.propagate(e);
                }
            }
            return;
        }

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
            hasOldValue = action.hasOldValue;
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

    public void setUpdateEditValueCallback(Callback<Object> updateEditValueCallback) {
        this.updateEditValueCallback = updateEditValueCallback;
    }
}
