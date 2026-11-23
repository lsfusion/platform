package lsfusion.gwt.client.form.controller;

import com.google.gwt.core.client.JavaScriptObject;
import lsfusion.gwt.client.action.GControllerResultAction;
import lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher;
import lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher.ServerResponseCallback;
import lsfusion.gwt.client.form.controller.dispatch.ExceptionResult;
import lsfusion.gwt.client.form.property.JSValueConverter;
import lsfusion.gwt.client.form.property.PValue;

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

    public final JavaScriptObject extendController(JavaScriptObject contextController) {
        return extendController(controller, contextController);
    }

    // a context builds its own controller out of what it adds and what every context can do, taking the latter from
    // here rather than listing it, so that a method added above reaches every context on its own. The result must keep
    // them as OWN enumerable properties - application code spreads and enumerates the controller - hence a copy of
    // Object.keys rather than inheritance or a for-in (which would also copy a polluted Object.prototype)
    public static native JavaScriptObject extendController(JavaScriptObject base, JavaScriptObject contextController) /*-{
        Object.keys(base).forEach(function (name) {
            contextController[name] = base[name];
        });
        return contextController;
    }-*/;

    private native JavaScriptObject initController() /*-{
        var thisObj = this;
        // the calls of a kind: the options of a call - their JSON text, null for none -, and what the arguments after
        // the first go as
        var calls = function (options, encodeParams) {
            return {
                exec: function (action) { // exec(action, ...params) -> Promise
                    return thisObj.@GController::controllerExec(*)(action, encodeParams(arguments), options);
                },
                eval: function (script) { // eval(script, ...params) -> Promise; script defines its own run action (typed params), result via return
                    return thisObj.@GController::controllerEval(*)(script, false, encodeParams(arguments), options);
                },
                evalAction: function (script) { // evalAction(script, ...params) -> Promise; script is an action body, auto-wrapped into run(), result via return
                    return thisObj.@GController::controllerEval(*)(script, true, encodeParams(arguments), options);
                }
            };
        };
        var controller = calls(null, function (args) {
            return Array.prototype.slice.call(args, 1);
        });
        controller.change = function (property) { // change(property, ...keyParams, value) -> Promise; value is the LAST argument
            var args = Array.prototype.slice.call(arguments, 1);
            var value = args.length > 0 ? args[args.length - 1] : null;
            var keyParams = args.length > 0 ? args.slice(0, args.length - 1) : args;
            return thisObj.@GController::controllerChange(*)(property, keyParams, value);
        };
        // mcp.exec / eval / evalAction: the same calls made as the MCP eval tool's - the arguments are the tool's
        // params and the call resolves with what the tool answers, {results, status}. The params go as one argument,
        // the JSON text of them all, which the server reads as the tool reads its own: a number is not the double it
        // would go as
        controller.mcp = calls(JSON.stringify({ mcp: true }), function (args) {
            return [JSON.stringify(Array.prototype.slice.call(args, 1))];
        });
        return controller;
    }-*/;

    public JavaScriptObject controllerExec(String action, JavaScriptObject params, String options) {
        String refusal = getRefusal("exec()");
        if (refusal != null) // reject BEFORE dispatch: a dispatched action on a closed context enqueues but never executes, leaking the queue entry / busy state
            return rejectedControllerPromise(refusal);
        ArrayList<Serializable> encoded = JSValueConverter.encodeUnknownJSValues(params);
        return createControllerPromise(dispatchExec(action, encoded, options, new ControllerServerResponseCallback()));
    }
    public JavaScriptObject controllerEval(String script, boolean evalAction, JavaScriptObject params, String options) {
        String refusal = getRefusal(evalAction ? "evalAction()" : "eval()");
        if (refusal != null)
            return rejectedControllerPromise(refusal);
        ArrayList<Serializable> encoded = JSValueConverter.encodeUnknownJSValues(params);
        return createControllerPromise(dispatchEval(script, evalAction, encoded, options, new ControllerServerResponseCallback()));
    }
    public JavaScriptObject controllerChange(String property, JavaScriptObject keyParams, JavaScriptObject value) {
        String refusal = getRefusal("change()");
        if (refusal != null)
            return rejectedControllerPromise(refusal);
        ArrayList<Serializable> encodedKeys = JSValueConverter.encodeUnknownJSValues(keyParams);
        Serializable encodedValue = JSValueConverter.encodeUnknownJSValue(value);
        return createControllerPromise(dispatchChange(property, encodedKeys, encodedValue, new ControllerServerResponseCallback()));
    }
    // why the context refuses a call now, or null: a closed one refuses every call
    protected String getRefusal(String surface) {
        return getClosedRefusal();
    }
    // ... and why it refuses any request at all - a change too, which a batch carries -: it is closed
    protected String getClosedRefusal() {
        return isClosed() ? CLOSED_REFUSAL : null;
    }
    public static final String CLOSED_REFUSAL = "Form is closed";
    public static native JavaScriptObject rejectedControllerPromise(String message) /*-{
        return $wnd.Promise.reject(new $wnd.Error(message));
    }-*/;

    // context-specific transport: build the request action and dispatch it; returns the assigned requestIndex (the promise key)
    protected abstract long dispatchExec(String action, ArrayList<Serializable> params, String options, ServerResponseCallback callback);
    protected abstract long dispatchEval(String script, boolean evalAction, ArrayList<Serializable> params, String options, ServerResponseCallback callback);
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

    // what a call resolves with: the value of a result without parameters, every value with its name of a result with them
    public static JavaScriptObject getResult(GControllerResultAction action) {
        if (action.names == null)
            return getResult(action, 0);
        JavaScriptObject results = JavaScriptObject.createArray();
        for (int i = 0; i < action.names.length; i++)
            addResult(results, action.names[i], getResult(action, i));
        return results;
    }
    private static JavaScriptObject getResult(GControllerResultAction action, int i) {
        return JSValueConverter.convertToJSValue(action.types[i], null, false, PValue.convertFileValue(action.values[i]));
    }
    private static native void addResult(JavaScriptObject results, String name, JavaScriptObject value) /*-{
        results.push({ name: name, value: value == null ? undefined : value }); // as a value alone resolves
    }-*/;

    public void controllerCallbackResult(long requestIndex, JavaScriptObject result) {
        resolveControllerCallback(controllerCallbacks, requestIndex, result);
    }
    // whether a call waited for it
    public boolean controllerCallbackException(long requestIndex, String message, boolean cancelled) {
        return rejectControllerCallback(controllerCallbacks, requestIndex, message, cancelled);
    }
    private static native void resolveControllerCallback(JavaScriptObject callbacks, double requestIndex, JavaScriptObject result) /*-{
        var cb = callbacks[requestIndex];
        if(cb) {
            delete callbacks[requestIndex];
            cb.resolve(result == null ? undefined : result); // no/null server value -> JS undefined
        }
    }-*/;
    private static native boolean rejectControllerCallback(JavaScriptObject callbacks, double requestIndex, String message, boolean cancelled) /*-{
        var cb = callbacks[requestIndex];
        if(cb) {
            delete callbacks[requestIndex];
            var error = new Error(message || "Cancelled");
            error.cancelled = cancelled;
            cb.reject(error);
            return true;
        }
        return false;
    }-*/;
}
