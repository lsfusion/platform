package lsfusion.gwt.client.form.controller.dispatch;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.action.*;
import lsfusion.gwt.client.base.Result;
import lsfusion.gwt.client.base.log.GLog;
import lsfusion.gwt.client.base.view.DialogBoxHelper;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.WindowHiddenHandler;
import lsfusion.gwt.client.classes.GObjectClass;
import lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher;
import lsfusion.gwt.client.controller.remote.action.RequestAsyncCallback;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.form.classes.view.ClassChosenHandler;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EndReason;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;
import lsfusion.gwt.client.navigator.window.GModalityShowFormType;
import lsfusion.gwt.client.view.MainFrame;
import lsfusion.interop.action.ServerResponse;

public class GFormActionDispatcher extends GwtActionDispatcher {
    protected final GFormController form;

    public GFormActionDispatcher(GFormController form) {
        this.form = form;
    }

    @Override
    protected void continueServerInvocation(long requestIndex, Object[] actionResults, int continueIndex, RequestAsyncCallback<ServerResponseResult> callback) {
        form.continueServerInvocation(requestIndex, actionResults, continueIndex, callback);
    }

    @Override
    protected void throwInServerInvocation(long requestIndex, Throwable t, int continueIndex, RequestAsyncCallback<ServerResponseResult> callback) {
        form.throwInServerInvocation(requestIndex, t, continueIndex, callback);
    }

    @Override
    public boolean canShowDockedModal() {
        return !form.isWindow();
    }

    @Override
    public void execute(final GFormAction action) {
        if (action.showFormType.isDockedModal() && !canShowDockedModal()) {
            action.showFormType = GModalityShowFormType.MODAL;
        }

        if (action.showFormType.isModal()) {
            pauseDispatching();
        }
        WindowHiddenHandler onClose = () -> {
            if (action.showFormType.isModal()) {
                continueDispatching();
            }
        };
        try {
            form.openForm(getDispatchingIndex(), action.form, action.showFormType, action.forbidDuplicate, editEventHandler != null ? editEventHandler.event : null, editContext, onClose, action.formId);
        } catch (Throwable t) {
            onClose.onHidden();
            throw t;
        }

    }

    @Override
    protected void onServerInvocationResponse(ServerResponseResult response) {
        form.onServerInvocationResponse(response);
    }

    @Override
    public Object execute(GChooseClassAction action) {
        pauseDispatching();
        Result<Object> result = new Result<>();
        form.showClassDialog(action.baseClass, action.defaultClass, action.concreate, new ClassChosenHandler() {
            @Override
            public void onClassChosen(GObjectClass chosenClass) {
                continueDispatching(chosenClass == null ? null : chosenClass.ID, result);
            }
        });
        return result.result;
    }

    @Override
    public Object execute(GConfirmAction action) {
        pauseDispatching();

        Result<Object> result = new Result<>();
        form.blockingConfirm(action.caption, action.message, action.cancel, action.timeout, action.initialValue,
                chosenOption -> continueDispatching(chosenOption.asInteger(), result));
        return result.result;
    }

    @Override
    public void execute(GLogMessageAction action) {
        if (GLog.isLogPanelVisible || action.failed) {
            super.execute(action);
        } else {
            pauseDispatching();
            form.blockingMessage(action.failed, "lsFusion", action.message, new DialogBoxHelper.CloseCallback() {
                @Override
                public void closed(DialogBoxHelper.OptionType chosenOption) {
                    continueDispatching();
                }
            });
        }
    }

    @Override
    public void execute(GHideFormAction action) {
        form.hideForm(getAsyncFormController(getDispatchingIndex()), action.closeDelay, editFormCloseReason);
    }

    @Override
    public void execute(GProcessFormChangesAction action) {
        form.applyRemoteChanges(action.formChanges);
    }

    @Override
    public void execute(GAsyncGetRemoteChangesAction action) {
        form.getRemoteChanges(action.forceLocalEvents);
    }

    //todo: по идее, action должен заливать куда-то в сеть выбранный локально файл
    @Override
    public String execute(GLoadLinkAction action) {
        return null;
    }

    @Override
    public void execute(GResetWindowsLayoutAction action) {
        if (!MainFrame.mobile) {
            form.resetWindowsLayout();
        }
    }

    // editing (INPUT) functionality

    public EventHandler editEventHandler;
    public EditContext editContext; // needed for some input operations (input, update edit value)

    public EndReason editFormCloseReason;

    @Override
    public Object execute(GRequestUserInputAction action) {

        pauseDispatching();

        // we should not drop at least editSetValue since GUpdateEditValueAction might use it
        Result<Object> result = new Result<>();
        // we'll be optimists and assume that this value will stay
        long dispatchingIndex = getDispatchingIndex();
        form.edit(action.readType, editEventHandler, action.hasOldValue, action.oldValue, action.inputList,
                (value, onExec) -> {
                    onExec.accept(dispatchingIndex);

                    continueDispatching(value, result);
                },
                (cancelReason) -> continueDispatching(GUserInputResult.canceled, result), editContext, ServerResponse.INPUT, null);
        return result.result;
    }

    @Override
    public void execute(GUpdateEditValueAction action) {
        if(editContext != null) {
            form.setValue(editContext, action.value);
        }
    }

    @Override
    public void execute(GCloseFormAction action) {
        form.getFormsController().closeForm(action.formId);
    }
}
