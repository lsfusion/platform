package lsfusion.gwt.client.navigator.controller.dispatch;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.NativeEvent;
import lsfusion.gwt.client.action.*;
import lsfusion.gwt.client.base.GwtClientUtils;
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
import lsfusion.gwt.client.form.property.JSValueConverter;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.controller.GNavigatorController;
import lsfusion.gwt.client.view.MainFrame;

import java.io.Serializable;
import java.util.ArrayList;

public class GNavigatorActionDispatcher extends GwtActionDispatcher {
    
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

    public GNavigatorActionDispatcher(FormsController formsController, GNavigatorController navigatorController) {
        this.formsController = formsController;
        this.navigatorController = navigatorController;
        // a custom navigator view hands this to its component; field initializers have all run by now, so it is ready
        navigatorController.setController(controller);
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
    public Object execute(GRequestUserInputAction action) {
        // the navigator has no cell to ask for a value in - what asks for it is a navigator action or a call of its
        // controller: the input is cancelled, as a user cancels one, and the action asking for it goes on as it then would
        // (an INPUT's ELSE)
        GwtClientUtils.consoleError("an input is cancelled, as a user cancels one: there is no cell to enter it in, since what asked for it is a navigator action or a call of the navigator's controller");
        return GUserInputResult.canceled;
    }

    @Override
    protected void onServerInvocationResponse(ServerResponseResult response) {
        formsController.onServerInvocationResponse(response, getAsyncFormController(response.requestIndex));
    }

    @Override
    protected void onServerInvocationFailed(ExceptionResult exceptionResult) {
        formsController.onServerInvocationFailed(getAsyncFormController(exceptionResult.requestIndex));
    }

    // the shared exec/eval/change plus activate, which is not on the base GController since a form has no navigator
    // elements to activate
    private final JavaScriptObject controller = initController();
    private native JavaScriptObject initController() /*-{
        var thisObj = this;
        return this.@GNavigatorActionDispatcher::gController.@lsfusion.gwt.client.form.controller.GController::extendController(Lcom/google/gwt/core/client/JavaScriptObject;)({
            // activate(canonicalName[, event]) - the event is optional, since a caller may have no browser event at
            // all, and may be React's synthetic one: passing that straight through would read its Ctrl modifier fine
            // but lose the browser event the optimistic form open wants, so unwrap it here
            activate: function (canonicalName, event) {
                var browserEvent = event && event.nativeEvent ? event.nativeEvent : (event || null);
                var error = thisObj.@GNavigatorActionDispatcher::activate(Ljava/lang/String;Lcom/google/gwt/dom/client/NativeEvent;)(canonicalName, browserEvent);
                if (error) throw new Error(error);
            }
        });
    }-*/;

    private String activate(String canonicalName, NativeEvent event) {
        GNavigatorElement element = navigatorController.getElement(canonicalName);
        if (element == null)
            return "Navigator element '" + canonicalName + "' not found";
        // SHOWIF is not a security boundary, but activating by name must not reach past what the navigator shows - and it
        // shows no element that is hidden, or that is in a hidden folder (GAbstractNavigatorView.forEachDrawn)
        for (GNavigatorElement up = element; up != null; up = up.parent)
            if (up.hide)
                return "Navigator element '" + canonicalName + "' is hidden"
                        + (up != element ? ", as its folder '" + up.canonicalName + "' is" : "");

        navigatorController.activate(element, event);
        return null;
    }

    @Override
    protected JavaScriptObject getController() {
        return controller;
    }

    @Override
    public void execute(GControllerResultAction action) {
        gController.controllerCallbackResult(getDispatchingIndex(), JSValueConverter.convertToJSValue(action.type, null, false, PValue.convertFileValue(action.value)));
    }

    @Override
    public void execute(GControllerExceptionAction action) {
        gController.controllerCallbackException(getDispatchingIndex(), action.message, action.cancelled);
    }

    @Override
    protected PopupOwner getPopupOwner() {
        return PopupOwner.GLOBAL;
    }
}
