package lsfusion.gwt.client.form.controller;

import com.google.gwt.core.client.JavaScriptObject;
import lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher;
import lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher.ServerResponseCallback;
import lsfusion.gwt.client.form.controller.dispatch.ExceptionResult;
import lsfusion.gwt.client.form.object.table.grid.view.GSimpleStateTableView;

import java.io.Serializable;
import java.util.ArrayList;

// Shared JS controller machinery for INTERNAL CLIENT / CUSTOM views: builds the `controller` object exposing
// exec(action,...params) / eval(script,...params) / change(property,...keyParams,value) as Promises, keeps the
// requestIndex -> {resolve,reject} registry, and resolves/rejects on the terminal GControllerResultAction /
// GControllerExceptionAction. The transport differs by context (form session vs navigator session): the dispatch*
// hooks build and send the context-specific request action; isClosed() lets a closed context reject pending
// promises. See GFORM-CONTROLLER-EXEC-EVAL-PLAN §5/§12.
public abstract class GController {

    public final JavaScriptObject controller = initController();

    // pending JS promise callbacks keyed by requestIndex; the call stores {resolve, reject} here and the terminal
    // GControllerResult/ExceptionAction (carrying the same requestIndex) resolves/rejects it.
    private final JavaScriptObject controllerCallbacks = JavaScriptObject.createObject();

    private native JavaScriptObject initController() /*-{
        var thisObj = this;
        return {
            exec: function (action) { // exec(action, ...params) -> Promise
                return thisObj.@GController::controllerExec(*)(action, Array.prototype.slice.call(arguments, 1));
            },
            eval: function (script) { // eval(script, ...params) -> Promise; script defines its own run action (typed params), result via return
                return thisObj.@GController::controllerEval(*)(script, false, Array.prototype.slice.call(arguments, 1));
            },
            evalAction: function (script) { // evalAction(script, ...params) -> Promise; script is an action body, auto-wrapped into run(), result via return
                return thisObj.@GController::controllerEval(*)(script, true, Array.prototype.slice.call(arguments, 1));
            },
            change: function (property) { // change(property, ...keyParams, value) -> Promise; value is the LAST argument
                var args = Array.prototype.slice.call(arguments, 1);
                var value = args.length > 0 ? args[args.length - 1] : null;
                var keyParams = args.length > 0 ? args.slice(0, args.length - 1) : args;
                return thisObj.@GController::controllerChange(*)(property, keyParams, value);
            }
        }
    }-*/;

    public JavaScriptObject controllerExec(String action, JavaScriptObject params) {
        if (isClosed()) // reject BEFORE dispatch: a dispatched action on a closed context enqueues but never executes, leaking the queue entry / busy state
            return rejectedControllerPromise();
        ArrayList<Serializable> encoded = GSimpleStateTableView.encodeUnknownJSValues(params);
        return createControllerPromise(dispatchExec(action, encoded, new ControllerServerResponseCallback()));
    }
    public JavaScriptObject controllerEval(String script, boolean evalAction, JavaScriptObject params) {
        if (isClosed())
            return rejectedControllerPromise();
        ArrayList<Serializable> encoded = GSimpleStateTableView.encodeUnknownJSValues(params);
        return createControllerPromise(dispatchEval(script, evalAction, encoded, new ControllerServerResponseCallback()));
    }
    public JavaScriptObject controllerChange(String property, JavaScriptObject keyParams, JavaScriptObject value) {
        if (isClosed())
            return rejectedControllerPromise();
        ArrayList<Serializable> encodedKeys = GSimpleStateTableView.encodeUnknownJSValues(keyParams);
        Serializable encodedValue = GSimpleStateTableView.encodeUnknownJSValue(value);
        return createControllerPromise(dispatchChange(property, encodedKeys, encodedValue, new ControllerServerResponseCallback()));
    }
    private native JavaScriptObject rejectedControllerPromise() /*-{
        return $wnd.Promise.reject(new $wnd.Error("Form is closed"));
    }-*/;

    // context-specific transport: build the request action and dispatch it; returns the assigned requestIndex (the promise key)
    protected abstract long dispatchExec(String action, ArrayList<Serializable> params, ServerResponseCallback callback);
    protected abstract long dispatchEval(String script, boolean evalAction, ArrayList<Serializable> params, ServerResponseCallback callback);
    protected abstract long dispatchChange(String property, ArrayList<Serializable> keyParams, Serializable value, ServerResponseCallback callback);
    protected abstract boolean isClosed();
    // the GwtActionDispatcher that processes the response actions (form: the form action dispatcher; navigator: the navigator dispatcher)
    protected abstract GwtActionDispatcher getControllerDispatcher();

    // dispatch already ran and assigned the requestIndex; register {resolve, reject} under it (synchronously, before the
    // response macrotask). If the context is already closed the dispatch was skipped -> reject so the Promise can't hang.
    private native JavaScriptObject createControllerPromise(double requestIndex) /*-{
        var self = this;
        var callbacks = this.@GController::controllerCallbacks;
        return new $wnd.Promise(function (resolve, reject) {
            callbacks[requestIndex] = { resolve: resolve, reject: reject };
            if(self.@GController::isClosed()())
                @GController::rejectControllerCallback(*)(callbacks, requestIndex, "Form is closed", false);
        });
    }-*/;

    // rejects the pending controller promise on transport/RPC failure (the terminal action would never arrive)
    protected class ControllerServerResponseCallback extends ServerResponseCallback {
        public ControllerServerResponseCallback() {
            super(false);
        }
        @Override
        protected GwtActionDispatcher getDispatcher() {
            return getControllerDispatcher();
        }
        @Override
        public void onFailure(ExceptionResult exceptionResult) {
            super.onFailure(exceptionResult);
            controllerCallbackException(exceptionResult.requestIndex, "Request failed", false);
        }
    }

    public void controllerCallbackResult(long requestIndex, JavaScriptObject result) {
        resolveControllerCallback(controllerCallbacks, requestIndex, result);
    }
    public void controllerCallbackException(long requestIndex, String message, boolean cancelled) {
        rejectControllerCallback(controllerCallbacks, requestIndex, message, cancelled);
    }
    private static native void resolveControllerCallback(JavaScriptObject callbacks, double requestIndex, JavaScriptObject result) /*-{
        var cb = callbacks[requestIndex];
        if(cb) {
            delete callbacks[requestIndex];
            cb.resolve(result == null ? undefined : result); // no/null server value -> JS undefined
        }
    }-*/;
    private static native void rejectControllerCallback(JavaScriptObject callbacks, double requestIndex, String message, boolean cancelled) /*-{
        var cb = callbacks[requestIndex];
        if(cb) {
            delete callbacks[requestIndex];
            var error = new Error(message || "Cancelled");
            error.cancelled = cancelled;
            cb.reject(error);
        }
    }-*/;
}
