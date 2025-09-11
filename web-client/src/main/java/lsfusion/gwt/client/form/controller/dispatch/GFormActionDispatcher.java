package lsfusion.gwt.client.form.controller.dispatch;

import com.google.gwt.core.client.JavaScriptObject;
import lsfusion.gwt.client.action.*;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.base.Result;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.base.view.DialogBoxHelper;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.PopupOwner;
import lsfusion.gwt.client.base.view.WindowHiddenHandler;
import lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher;
import lsfusion.gwt.client.controller.remote.action.RequestAsyncCallback;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.form.classes.view.GClassDialog;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.controller.CancelReason;
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
    protected void continueServerInvocation(long requestIndex, Object actionResult, int continueIndex, RequestAsyncCallback<ServerResponseResult> callback) {
        form.continueServerInvocation(requestIndex, actionResult, continueIndex, callback);
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

        executeAsyncNoResult(action.showFormType.isModal() && action.syncType, onResult -> {
            try {
                form.openForm(getDispatchingIndex(), action.form, action.showFormType, action.forbidDuplicate, action.syncType, editEventHandler != null ? editEventHandler.event : null, editContext, () -> onResult.accept(null), action.formId);
            } catch (Throwable t) {
                onResult.accept(t);
                throw t;
            }
        });
    }

    @Override
    protected void onServerInvocationResponse(ServerResponseResult response) {
        form.onServerInvocationResponse(response);
    }

    @Override
    protected void onServerInvocationFailed(ExceptionResult exceptionResult) {
        form.onServerInvocationFailed(exceptionResult);
    }

    @Override
    public Object execute(GChooseClassAction action) {
        return executeAsyncResult(onResult -> {
            GClassDialog.showDialog(action.baseClass, action.defaultClass, action.concreate, chosenClass -> onResult.accept(chosenClass == null ? null : chosenClass.ID, null), getPopupOwner());
        });
    }

    @Override
    protected PopupOwner getPopupOwner() {
        return editContext != null ? editContext.getPopupOwner() : form.getPopupOwner();
    }

    @Override
    public void execute(GHideFormAction action) {
        form.hideForm(getAsyncFormController(getDispatchingIndex()), editFormCloseReason != null ? editFormCloseReason : CancelReason.HIDE);
    }

    @Override
    public void execute(GDestroyFormAction action) {
        form.destroyForm(action.closeDelay);
    }

    @Override
    protected JavaScriptObject getController() {
        return form.controller;
    }

    @Override
    public void execute(GProcessFormChangesAction action) {
        form.applyRemoteChanges(action.formChanges);
    }

    @Override
    public void execute(GProcessNavigatorChangesAction action) {
        FormsController formsController = form.getFormsController();
        MainFrame.applyNavigatorChanges(action.navigatorChanges, formsController.getNavigatorController(), formsController.getWindowsController());
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

    @Override
    public void execute(GOrderAction action) {
        form.changePropertyOrder(action.goID, action.ordersMap);
    }

    @Override
    public void execute(GFilterAction action) {
        form.changePropertyFilters(action.goID, action.filters);
    }

    @Override
    public void execute(GFilterGroupAction action) {
        form.setRegularFilterIndex(action.filterGroup, action.index);
    }

    // editing (INPUT) functionality

    public EventHandler editEventHandler;
    public EditContext editContext; // needed for some input operations (input, update edit value)

    public EndReason editFormCloseReason;

    @Override
    public Object execute(GRequestUserInputAction action) {
        return executeAsyncResult(onResult -> {
            // we'll be optimists and assume that this value will stay
            long dispatchingIndex = getDispatchingIndex();
            form.edit(action.readType, editEventHandler, action.hasOldValue, PValue.convertFileValue(action.oldValue), action.inputList, action.inputListActions,
                    (value, onExec) -> {
                        onExec.accept(dispatchingIndex);

                        onResult.accept(value, null);
                    },
                    (cancelReason) -> onResult.accept(GUserInputResult.canceled, null), editContext, ServerResponse.INPUT, null);
        });
    }

    @Override
    public void execute(GUpdateEditValueAction action) {
        if(editContext != null) {
            form.setValue(editContext, PValue.convertFileValue(action.value));
        }
    }

    @Override
    public void execute(GCloseFormAction action) {
        form.getFormsController().closeForm(action.formId);
    }
}
