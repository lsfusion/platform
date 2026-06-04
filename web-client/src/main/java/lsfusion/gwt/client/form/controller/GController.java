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
// callbackId -> {resolve,reject} registry, and resolves/rejects on the terminal GControllerResultAction /
// GControllerExceptionAction. The transport differs by context (form session vs navigator session): the dispatch*
// hooks build and send the context-specific request action; isClosed() lets a closed context reject pending
// promises. See GFORM-CONTROLLER-EXEC-EVAL-PLAN §5/§12.
public abstract class GController {

    public final JavaScriptObject controller = initController();

    // pending JS promise callbacks keyed by callbackId; the call stores {resolve, reject} here and the terminal
    // GControllerResult/ExceptionAction resolves/rejects it.
    private final JavaScriptObject controllerCallbacks = JavaScriptObject.createObject();

    private long nextControllerCallbackId = 0;

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
        long callbackId = nextControllerCallbackId++;
        ArrayList<Serializable> encoded = GSimpleStateTableView.encodeUnknownJSValues(params);
        return createControllerPromise(callbackId, () -> dispatchExec(callbackId, action, encoded, new ControllerServerResponseCallback(callbackId)));
    }
    public JavaScriptObject controllerEval(String script, boolean evalAction, JavaScriptObject params) {
        long callbackId = nextControllerCallbackId++;
        ArrayList<Serializable> encoded = GSimpleStateTableView.encodeUnknownJSValues(params);
        return createControllerPromise(callbackId, () -> dispatchEval(callbackId, script, evalAction, encoded, new ControllerServerResponseCallback(callbackId)));
    }
    public JavaScriptObject controllerChange(String property, JavaScriptObject keyParams, JavaScriptObject value) {
        long callbackId = nextControllerCallbackId++;
        ArrayList<Serializable> encodedKeys = GSimpleStateTableView.encodeUnknownJSValues(keyParams);
        Serializable encodedValue = GSimpleStateTableView.encodeUnknownJSValue(value);
        return createControllerPromise(callbackId, () -> dispatchChange(callbackId, property, encodedKeys, encodedValue, new ControllerServerResponseCallback(callbackId)));
    }

    // context-specific transport: build the request action (set its callbackId) and dispatch it with the callback
    protected abstract void dispatchExec(long callbackId, String action, ArrayList<Serializable> params, ServerResponseCallback callback);
    protected abstract void dispatchEval(long callbackId, String script, boolean evalAction, ArrayList<Serializable> params, ServerResponseCallback callback);
    protected abstract void dispatchChange(long callbackId, String property, ArrayList<Serializable> keyParams, Serializable value, ServerResponseCallback callback);
    protected abstract boolean isClosed();
    // the GwtActionDispatcher that processes the response actions (form: the form action dispatcher; navigator: the navigator dispatcher)
    protected abstract GwtActionDispatcher getControllerDispatcher();

    // register {resolve, reject} under callbackId BEFORE kicking off the (async) dispatch, so the entry exists when
    // the terminal action is processed (single JS turn; the response is a later macrotask). If the context is
    // already closed the dispatch is skipped silently (no callback ever fires) -> reject so the Promise can't hang.
    private native JavaScriptObject createControllerPromise(double callbackId, Runnable dispatch) /*-{
        var self = this;
        var callbacks = this.@GController::controllerCallbacks;
        return new $wnd.Promise(function (resolve, reject) {
            callbacks[callbackId] = { resolve: resolve, reject: reject };
            dispatch.@java.lang.Runnable::run()();
            if(self.@GController::isClosed()())
                @GController::rejectControllerCallback(*)(callbacks, callbackId, "Form is closed", false);
        });
    }-*/;

    // rejects the pending controller promise on transport/RPC failure (the terminal action would never arrive)
    protected class ControllerServerResponseCallback extends ServerResponseCallback {
        private final long callbackId;
        public ControllerServerResponseCallback(long callbackId) {
            super(false);
            this.callbackId = callbackId;
        }
        @Override
        protected GwtActionDispatcher getDispatcher() {
            return getControllerDispatcher();
        }
        @Override
        public void onFailure(ExceptionResult exceptionResult) {
            super.onFailure(exceptionResult);
            controllerCallbackException(callbackId, "Request failed", false);
        }
    }

    public void controllerCallbackResult(long callbackId, JavaScriptObject result) {
        resolveControllerCallback(controllerCallbacks, callbackId, result);
    }
    public void controllerCallbackException(long callbackId, String message, boolean cancelled) {
        rejectControllerCallback(controllerCallbacks, callbackId, message, cancelled);
    }
    private static native void resolveControllerCallback(JavaScriptObject callbacks, double callbackId, JavaScriptObject result) /*-{
        var cb = callbacks[callbackId];
        if(cb) {
            delete callbacks[callbackId];
            cb.resolve(result == null ? undefined : result); // no/null server value -> JS undefined
        }
    }-*/;
    private static native void rejectControllerCallback(JavaScriptObject callbacks, double callbackId, String message, boolean cancelled) /*-{
        var cb = callbacks[callbackId];
        if(cb) {
            delete callbacks[callbackId];
            var error = new Error(message || "Cancelled");
            error.cancelled = cancelled;
            cb.reject(error);
        }
    }-*/;
}
