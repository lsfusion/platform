package lsfusion.gwt.client.navigator.controller.dispatch;

import com.google.gwt.core.client.JavaScriptObject;
import lsfusion.gwt.client.action.*;
import lsfusion.gwt.client.base.view.PopupOwner;
import lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher;
import lsfusion.gwt.client.controller.remote.action.RequestAsyncCallback;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.navigator.ContinueNavigatorAction;
import lsfusion.gwt.client.controller.remote.action.navigator.NavigatorControllerChangeAction;
import lsfusion.gwt.client.controller.remote.action.navigator.NavigatorControllerEvalAction;
import lsfusion.gwt.client.controller.remote.action.navigator.NavigatorControllerExecAction;
import lsfusion.gwt.client.controller.remote.action.navigator.ThrowInNavigatorAction;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GController;
import lsfusion.gwt.client.form.controller.dispatch.ExceptionResult;
import lsfusion.gwt.client.form.object.table.grid.view.GSimpleStateTableView;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.navigator.controller.GNavigatorController;
import lsfusion.gwt.client.navigator.window.view.WindowsController;
import lsfusion.gwt.client.view.MainFrame;

import java.io.Serializable;
import java.util.ArrayList;

public class GNavigatorActionDispatcher extends GwtActionDispatcher {
    
    private final WindowsController windowsController;
    private final FormsController formsController;
    private final GNavigatorController navigatorController;

    // INTERNAL CLIENT in navigator context gets exec/eval/change too — same shared machinery as the form, but the
    // request runs in the navigator's session (a fresh DataSession per call). The navigator has no per-form close,
    // so isClosed() is always false. See NAVIGATOR-CONTROLLER-PLAN.
    private final GController gController = new GController() {
        @Override
        protected long dispatchExec(String action, ArrayList<Serializable> params, GwtActionDispatcher.ServerResponseCallback callback) {
            return MainFrame.navigatorDispatchAsync.asyncExecute(new NavigatorControllerExecAction(action, params), callback);
        }
        @Override
        protected long dispatchEval(String script, boolean evalAction, ArrayList<Serializable> params, GwtActionDispatcher.ServerResponseCallback callback) {
            return MainFrame.navigatorDispatchAsync.asyncExecute(new NavigatorControllerEvalAction(script, evalAction, params), callback);
        }
        @Override
        protected long dispatchChange(String property, ArrayList<Serializable> keyParams, Serializable value, GwtActionDispatcher.ServerResponseCallback callback) {
            return MainFrame.navigatorDispatchAsync.asyncExecute(new NavigatorControllerChangeAction(property, keyParams, value), callback);
        }
        @Override
        protected boolean isClosed() {
            return false;
        }
        @Override
        protected GwtActionDispatcher getControllerDispatcher() {
            return GNavigatorActionDispatcher.this;
        }
    };

    public GNavigatorActionDispatcher(WindowsController windowsController, FormsController formsController, GNavigatorController navigatorController) {
        this.windowsController = windowsController;
        this.formsController = formsController;
        this.navigatorController = navigatorController;
    }

    @Override
    protected void throwInServerInvocation(long requestIndex, Throwable t, int continueIndex, RequestAsyncCallback<ServerResponseResult> callback) {
        MainFrame.syncDispatch(new ThrowInNavigatorAction(t, requestIndex, continueIndex), callback, true);
    }

    @Override
    protected void continueServerInvocation(long requestIndex, Object actionResult, int continueIndex, RequestAsyncCallback<ServerResponseResult> callback) {
        MainFrame.syncDispatch(new ContinueNavigatorAction(actionResult, requestIndex, continueIndex), callback, true);
    }

    @Override
    public void executeVoidAction(long requestIndex) {
        formsController.executeVoidAction(requestIndex);
    }

    @Override
    protected FormsController getFormsController() {
        return formsController;
    }

    @Override
    protected FormsController.OpenContext getOpenContext(GFormAction action) {
        return new FormsController.OpenContext(null, null, null, null);
    }

    @Override
    public void execute(GProcessNavigatorChangesAction action) {
        MainFrame.applyNavigatorChanges(action.navigatorChanges, navigatorController, windowsController);
    }

    @Override
    protected void onServerInvocationResponse(ServerResponseResult response) {
        formsController.onServerInvocationResponse(response, getAsyncFormController(response.requestIndex));
    }

    @Override
    protected void onServerInvocationFailed(ExceptionResult exceptionResult) {
        formsController.onServerInvocationFailed(getAsyncFormController(exceptionResult.requestIndex));
    }

    @Override
    public void execute(final GActivateFormAction action) {
        formsController.selectTab(action.formCanonicalName);
    }

    @Override
    public void execute(final GMaximizeFormAction action) {
        if (!MainFrame.mobile) {
            formsController.setFullScreenMode(true);
        }
    }

    @Override
    protected JavaScriptObject getController() {
        return gController.controller;
    }

    @Override
    public void execute(GControllerResultAction action) {
        gController.controllerCallbackResult(getDispatchingIndex(), GSimpleStateTableView.convertToJSValue(action.type, null, false, PValue.convertFileValue(action.value)));
    }

    @Override
    public void execute(GControllerExceptionAction action) {
        gController.controllerCallbackException(getDispatchingIndex(), action.message, action.cancelled);
    }

    @Override
    protected PopupOwner getPopupOwner() {
        return PopupOwner.GLOBAL;
    }

    @Override
    public void execute(final GCloseFormAction action) {
        formsController.closeForm(action.formId);
    }
}
