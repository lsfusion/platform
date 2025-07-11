package lsfusion.gwt.client.controller.dispatch;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.media.client.Audio;
import com.google.gwt.typedarrays.client.Uint8ArrayNative;
import com.google.gwt.typedarrays.shared.ArrayBuffer;
import com.google.gwt.typedarrays.shared.Uint8Array;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.xhr.client.XMLHttpRequest;
import lsfusion.gwt.client.action.*;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.base.exception.GExceptionManager;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeStringMap;
import lsfusion.gwt.client.base.log.GLog;
import lsfusion.gwt.client.base.view.DialogBoxHelper;
import lsfusion.gwt.client.base.view.PopupOwner;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.controller.remote.action.RequestAsyncCallback;
import lsfusion.gwt.client.controller.remote.action.RequestCountingErrorHandlingCallback;
import lsfusion.gwt.client.controller.remote.action.RequestErrorHandlingCallback;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.navigator.LogClientExceptionAction;
import lsfusion.gwt.client.form.controller.dispatch.ExceptionResult;
import lsfusion.gwt.client.form.object.table.grid.view.GSimpleStateTableView;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.form.view.FormDockable;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.view.MainFrame;

import java.util.*;

import static lsfusion.gwt.client.controller.remote.action.PriorityErrorHandlingCallback.showErrorMessage;

public abstract class GwtActionDispatcher implements GActionDispatcher {
    private boolean dispatchingPaused;

    protected ServerResponseResult currentResponse = null;
    Object[] currentActionResults = null;
    private int currentActionIndex = -1;
    private int currentContinueIndex = -1;
    private Runnable currentOnDispatchFinished = null;
    private Runnable currentOnRequestFinished = null;

    protected abstract void onServerInvocationResponse(ServerResponseResult response);
    protected abstract void onServerInvocationFailed(ExceptionResult exceptionResult);

    public static abstract class ServerResponseCallback extends RequestCountingErrorHandlingCallback<ServerResponseResult> {

        private final boolean disableForbidDuplicate;

        public ServerResponseCallback(boolean disableForbidDuplicate) {
            this.disableForbidDuplicate = disableForbidDuplicate;
        }

        protected abstract GwtActionDispatcher getDispatcher();

        protected Runnable getOnRequestFinished() {
            return null;
        }

        public boolean canBeDispatched() {
            return !getDispatcher().dispatchingPaused;
        }

        @Override
        public void onSuccess(ServerResponseResult result, Runnable onDispatchFinished) {
            if (disableForbidDuplicate) {
                for (GAction action : result.actions) // it's a hack, but the whole forbidDuplicate mechanism is a one big hack
                    if (action instanceof GFormAction)
                        ((GFormAction) action).forbidDuplicate = false;
            }
            getDispatcher().dispatchServerResponse(result, onDispatchFinished, getOnRequestFinished());
        }

        @Override
        public void onFailure(ExceptionResult exceptionResult) {
            getDispatcher().dispatchServerFailed(exceptionResult, getOnRequestFinished());
        }
    }

    public void dispatchServerResponse(ServerResponseResult response, Runnable onDispatchFinished, Runnable onRequestFinished) {
        dispatchServerResponse(response, -1, onDispatchFinished, onRequestFinished);
    }
    public void dispatchServerResponse(ServerResponseResult response, int continueIndex, Runnable onDispatchFinished, Runnable onRequestFinished) {
        assert response != null;
        assert !dispatchingPaused;
        onServerInvocationResponse(response);

        dispatchResponse(response, continueIndex, onDispatchFinished, onRequestFinished);
    }
    public void continueDispatchResponse() {
        assert dispatchingPaused;
        dispatchResponse(null, -1, null, null);
    }
    public void dispatchResponse(ServerResponseResult response, int continueIndex, Runnable onDispatchFinished, Runnable onRequestFinished) {
        Object[] actionResults;
        Throwable actionThrowable = null;
        int beginIndex;
        if (dispatchingPaused) { // continueDispatching
            beginIndex = currentActionIndex + 1;
            actionResults = currentActionResults;
            continueIndex = currentContinueIndex;
            response = currentResponse;
            onDispatchFinished = currentOnDispatchFinished;
            onRequestFinished = currentOnRequestFinished;

            currentActionIndex = -1;
            currentContinueIndex = -1;
            currentActionResults = null;
            currentResponse = null;
            currentOnDispatchFinished = null;
            currentOnRequestFinished = null;
            dispatchingPaused = false;
        } else {
            beginIndex = 0;
            actionResults = new Object[response.actions.length];
        }

        for (int i = beginIndex; i < response.actions.length; i++) {
            GAction action = response.actions[i];
            Object dispatchResult;
            try {
                dispatchingIndex = response.requestIndex;
                try {
                    //for unsupported actions null is send to preserve number of actions and thus the order of responses
                    dispatchResult = action == null ? null : action.dispatch(this);
                } finally {
                    dispatchingIndex = -1;
                }
            } catch (Throwable ex) {
                actionThrowable = ex;
                break;
            }

            if (dispatchingPaused) {
                currentResponse = response;
                currentActionResults = actionResults;
                currentActionIndex = i;
                currentContinueIndex = continueIndex;
                currentOnDispatchFinished = onDispatchFinished;
                currentOnRequestFinished = onRequestFinished;
                return;
            }

            actionResults[i] = dispatchResult;
        }

        if(onDispatchFinished != null)
            onDispatchFinished.run();

        if (response.resumeInvocation) {
            continueIndex++;

            final int fContinueIndex = continueIndex;
            Runnable fOnRequestFinished = onRequestFinished;
            RequestErrorHandlingCallback<ServerResponseResult> continueRequestCallback =
                    new RequestErrorHandlingCallback<ServerResponseResult>() {
                        @Override
                        public void onSuccess(ServerResponseResult response, Runnable onDispatchFinished) {
                            dispatchServerResponse(response, fContinueIndex, onDispatchFinished, fOnRequestFinished);
                        }

                        @Override
                        public void onFailure(ExceptionResult exceptionResult) {
                            dispatchServerFailed(exceptionResult, fOnRequestFinished);
                        }
                    };
            if (actionThrowable == null) {
                continueServerInvocation(response.requestIndex, actionResults, continueIndex, continueRequestCallback);
            } else {
                throwInServerInvocation(response.requestIndex, LogClientExceptionAction.fromWebClientToWebServer(actionThrowable), continueIndex, continueRequestCallback);
            }
        } else {
            if(onRequestFinished != null)
                onRequestFinished.run();

            if (actionThrowable != null)
                throw GExceptionManager.propagate(actionThrowable);
        }
    }

    public void dispatchServerFailed(ExceptionResult exceptionResult, Runnable onRequestFinished) {
        onServerInvocationFailed(exceptionResult);

        showErrorMessage(exceptionResult.throwable, getPopupOwner()); // need this before to have editContext filled

        dispatchFailed(onRequestFinished);
    }

    public void dispatchFailed(Runnable onRequestFinished) {
        if (onRequestFinished != null)
            onRequestFinished.run();
    }

    protected abstract void throwInServerInvocation(long requestIndex, Throwable t, int continueIndex, RequestAsyncCallback<ServerResponseResult> callback);

    protected abstract void continueServerInvocation(long requestIndex, Object[] actionResults, int continueIndex, RequestAsyncCallback<ServerResponseResult> callback);

    // synchronization is guaranteed pretty tricky
    // in RemoteDispatchAsync there is a linked list q of all executing actions, where all responses are queued, and all continue invoications are put into it's beginning
    protected final void pauseDispatching() {
        dispatchingPaused = true;
    }

    public void continueDispatching() {
        continueDispatching(null, null);
    }

    protected long dispatchingIndex = -1;
    public long getDispatchingIndex() {
        if (currentResponse == null) // means that we continueDispatching before exiting to dispatchResponse cycle (for example LogicalCellRenderer commits editing immediately)
            return dispatchingIndex;
        else
            return currentResponse.requestIndex;
    }

    public void continueDispatching(Object currentActionResult, Result<Object> result) {
        assert dispatchingPaused;
        if (currentResponse == null) { // means that we continueDispatching before exiting to dispatchResponse cycle (for example LogicalCellRenderer commits editing immediately)
            // in this case we have to return result as no pauseDispatching happened
            dispatchingPaused = false;
            if(result != null)
                result.set(currentActionResult);
        } else {
            if (currentActionResults != null && currentActionIndex >= 0) {
                currentActionResults[currentActionIndex] = currentActionResult;
            }
            continueDispatchResponse();
        }
    }

    protected abstract PopupOwner getPopupOwner();

    public boolean canShowDockedModal() {
        return true;
    }

    @Override
    public void execute(GFormAction action) {
    }

    @Override
    public void execute(GReportAction action) {
        GwtClientUtils.openFile(action.reportFileName, action.autoPrint, action.autoPrintTimeout);
    }

    @Override
    public Object execute(GChooseClassAction action) {
        return null;
    }

    @Override
    public void execute(GMessageAction action) {
        boolean ifNotFocused = false;

        boolean log = action.type == GMessageType.LOG;
        boolean info = action.type == GMessageType.INFO;
        boolean success = action.type == GMessageType.SUCCESS;
        boolean warn = action.type == GMessageType.WARN;
        boolean error = action.type == GMessageType.ERROR;

        StaticImage image;
        String backgroundClass;
        if(info) {
            image = StaticImage.MESSAGE_INFO;
            backgroundClass = "bg-info-subtle";
        } else if(success) {
            image = StaticImage.MESSAGE_SUCCESS;
            backgroundClass = "bg-success-subtle";
        } else if(warn) {
            image = StaticImage.MESSAGE_WARN;
            backgroundClass = "bg-warning-subtle";
        } else if(error) {
            image = StaticImage.MESSAGE_WARN;
            backgroundClass = "bg-danger-subtle";
        } else { //default
            image = null;
            backgroundClass = null;
        }

        String message = PValue.getStringValue(PValue.convertFileValue(action.message));
        if(!log && !info) {
            if (action.syncType)
                pauseDispatching();
            DialogBoxHelper.showMessageBox(action.caption, GLog.toPrintMessage(message, image, action.data, action.titles), backgroundClass, getPopupOwner(), chosenOption -> {
                if (action.syncType)
                    continueDispatching();
            });

            ifNotFocused = true;
        }

        if (!log && !MainFrame.enableShowingRecentlyLogMessages)
            GLog.showFocusNotification(action.textMessage, action.caption, ifNotFocused);

        if(log || info || error) {
            Widget widget = action.data.isEmpty() ? EscapeUtils.toHTML(message, image) : GLog.toPrintMessage(message, image, action.data, action.titles);
            GwtClientUtils.addClassNames(widget, "alert", (log || info ? "alert-info" : "alert-danger"));
            GLog.message(widget, action.caption, error);
        }
    }

    @Override
    public Object execute(GConfirmAction action) {
        pauseDispatching();

        Result<Object> result = new Result<>();
        DialogBoxHelper.showConfirmBox(action.caption, EscapeUtils.toHTML(action.message, StaticImage.MESSAGE_WARN), action.cancel, action.timeout, action.initialValue, getPopupOwner(),
                chosenOption -> continueDispatching(chosenOption.asInteger(), result));
        return result.result;
    }

    @Override
    public void execute(GHideFormAction action) {
    }

    @Override
    public void execute(GDestroyFormAction action) {
    }

    @Override
    public void execute(GProcessFormChangesAction action) {
    }

    @Override
    public Object execute(GRequestUserInputAction action) {
        return null;
    }

    @Override
    public void execute(GUpdateEditValueAction action) {
    }

    @Override
    public void execute(GAsyncGetRemoteChangesAction action) {
        assert false;
    }

    @Override
    public void execute(GLogOutAction action) {
        GwtClientUtils.logout();
    }

    @Override
    public void execute(GOpenUriAction action) {
        String url = PValue.getStringValue(PValue.convertFileValue(action.uri));
        // we don't need to encode because it seems that the browser encodes the url itself
//        if(!action.noEncode)
//            url = URL.encodeQueryString(url);
        Window.open(url, "_blank", "");
    }

    @Override
    public void execute(GEditNotPerformedAction action) {
    }

    @Override
    public void execute(GOpenFileAction action) {
        GwtClientUtils.openFile(action.fileUrl, false, null);
    }

    @Override
    public void execute(GWriteAction action) {
        GwtClientUtils.downloadFile(action.fileUrl);
    }

    //todo: по идее, action должен заливать куда-то в сеть выбранный локально файл
    @Override
    public String execute(GLoadLinkAction action) {
        return null;
    }

    @Override
    public void execute(final GBeepAction action) {
        if(action.filePath != null) {
            String fileUrl = GwtClientUtils.getAppDownloadURL(action.filePath);
            Audio beep = Audio.createIfSupported();
            if (beep != null) {
                beep.setSrc(fileUrl);
                beep.play();
            }
        }
    }

    @Override
    public void execute(GActivateFormAction action) {
    }

    @Override
    public void execute(GMaximizeFormAction action) {
    }

    @Override
    public void execute(GCloseFormAction action) {
    }

    @Override
    public void execute(GChangeColorThemeAction action) {
        MainFrame.changeColorTheme(action.colorTheme);
    }

    @Override
    public void execute(GResetWindowsLayoutAction action) {
    }

    @Override
    public void execute(GOrderAction action) {
    }

    @Override
    public void execute(GFilterAction action) {
    }

    @Override
    public void execute(GFilterGroupAction action) {
    }

    protected abstract JavaScriptObject getController();

    private class JSExecutor {
        private final List<GClientWebAction> actions = new ArrayList<>();

        public Object addAction(GClientWebAction action) {
            actions.add(action);

            boolean syncType = action.syncType;
            if (syncType)
                pauseDispatching();

            flush();

            if(syncType)
                return action.execResult;

            return null;
        }

        private boolean isExecuting = false;
        private void flush() {
            if (!isExecuting && !actions.isEmpty()) {
                GClientWebAction action = actions.get(0);
                isExecuting = true;
                if (action.isFile)
                    executeFile(action);
                else
                    executeJSFunction(action);
            }
        }

        private final NativeStringMap<String> resources = new NativeStringMap<>();
        private void executeFile(GClientWebAction action) {
            String resource = action.resource;
            if(!action.isFileUrl)
                resource = GwtClientUtils.getAppStaticWebURL(resource);
            if(action.remove) {
                unloadResource(resource, action.resourceName, action.extension);
                resources.remove(resource);
                onFileExecuted(action);
            } else {
                if (resources.containsKey(resource)) {
                    onFileExecuted(action);
                } else {
                    resources.put(resource, null);
                    executeFile(action, resource, action.resourceName, action.extension);
                }
            }
        }

        private native void unloadResource(String resourcePath, String resourceName, String extension)/*-{
            if (extension === 'js') {
                var scripts = $wnd.document.head.getElementsByTagName("script");
                for (var i = 0; i < scripts.length; i++) {
                    var script = scripts[i];
                    if (script.src.indexOf(resourcePath) > 0)
                        script.parentNode.removeChild(script);
                }
            } else if (extension === 'css') {
                var links = $wnd.document.head.getElementsByTagName("link");
                for (var i = 0; i < links.length; i++) {
                    var link = links[i];
                    if (link.href.indexOf(resourcePath) > 0)
                        link.parentNode.removeChild(link);
                }
            } else if (extension === 'ttf' || extension === 'otf') {
                var fonts = document.fonts;
                for (var i = 0; i < fonts.size; i++) {
                    var font = fonts.values().next().value;
                    if (font.family === resourceName)
                        fonts['delete'](font);
                }
            } else {
                if ($wnd.lsfFiles != null && $wnd.lsfFiles[resourceName] != null)
                    delete $wnd.lsfFiles[resourceName];
            }
        }-*/;

        // should be pretty similar to WriteResourcesJSPTag
        private native void executeFile(GClientWebAction action, String resourcePath, String resourceName, String extension)/*-{
            var thisObj = this;

            if (extension === 'js') {
                var scr = document.createElement('script');
                scr.src = resourcePath;
                scr.type = 'text/javascript';
                $wnd.document.head.appendChild(scr);
                scr.onload = function() {thisObj.@JSExecutor::onFileExecuted(*)(action); }
                //temp fix: continueDispatching if .js not found
                scr.onerror = function() {thisObj.@JSExecutor::onFileExecuted(*)(action); }
            } else if (extension === 'css') {
                var link = document.createElement("link");
                link.href = resourcePath;
                link.type = "text/css";
                link.rel = "stylesheet";
                $wnd.document.head.appendChild(link);
                thisObj.@JSExecutor::onFileExecuted(*)(action);
            } else if (extension === 'ttf' || extension === 'otf') {
                var fontFace = new FontFace(resourceName, 'url(' + resourcePath + ')');
                fontFace.load().then(function (loaded_face) {
                    document.fonts.add(fontFace);
                    thisObj.@JSExecutor::onFileExecuted(*)(action);
                });
            } else {
                $wnd.lsfFiles[resourceName] = resourcePath;
                thisObj.@JSExecutor::onFileExecuted(*)(action);
            }
        }-*/;

        private void onFileExecuted(GClientWebAction action) {
            onActionExecuted(action, null);
        }

        private void onActionExecuted(GClientWebAction action, Object currentActionResult) {
            isExecuting = false;
            actions.remove(action);

            if (action.syncType) {
                Result<Object> result = new Result<>();
                continueDispatching(currentActionResult, result);
                action.execResult = result.result;
            }

            flush();
        }

        private native JavaScriptObject getOnResult(GClientWebAction action) /*-{
            var thisObj = this;
            return function (value) {
                return thisObj.@JSExecutor::onJSFunctionExecuted(*)(action, value);
            }
        }-*/;

        private JavaScriptObject getController() {
            return GwtActionDispatcher.this.getController();
        }
        private void onJSFunctionExecuted(GClientWebAction action, JavaScriptObject result) {
            onActionExecuted(action, PValue.convertFileValueBack(GSimpleStateTableView.convertFromJSValue(action.returnType, result)));
        }

        private void executeJSFunction(GClientWebAction action) {
            JavaScriptObject result = null;
            boolean async = false;
            try {
                JavaScriptObject fnc = GwtClientUtils.getGlobalField(action.resource);

                int fncParams = GwtClientUtils.getParamsCount(fnc);

                JsArray<JavaScriptObject> arguments = JavaScriptObject.createArray().cast();
                ArrayList<Object> types = action.types;
                for (int i = 0; i < types.size(); i++) {
                    arguments.push(GSimpleStateTableView.convertToJSValue((GType) types.get(i), null, true, PValue.convertFileValue(action.values.get(i))));
                }
                arguments.push(getController());

                if(fncParams > arguments.length()) { // takes onResult
                    async = true;
                    arguments.push(getOnResult(action));
                }

                result = GwtClientUtils.call(fnc, arguments);
            } finally {
                if(!async)
                    onJSFunctionExecuted(action, result);
            }
        }
    }

    private JSExecutor jsExecutor;
    @Override
    public Object execute(GClientWebAction action) {
        if (jsExecutor == null)
            jsExecutor = new JSExecutor();
        return jsExecutor.addAction(action);
    }

    @Override
    public Object execute(GHttpClientAction action) {
        pauseDispatching();
        XMLHttpRequest request = XMLHttpRequest.create();
        request.open(action.method.name(), action.connectionString);
        request.setResponseType("arraybuffer");
        for(Map.Entry<String, String> header : action.headers.entrySet()) {
            request.setRequestHeader(header.getKey(), header.getValue());
        }
        sendRequest(request, action.body != null ? bytesToArrayBuffer(action.body) : null);

        Result<Object> result = new Result<>();
        request.setOnReadyStateChange(xhr -> {
            if(xhr.getReadyState() == XMLHttpRequest.DONE) {
                ArrayBuffer arrayBuffer = xhr.getResponseArrayBuffer();
                byte[] bytes = arrayBufferToBytes(arrayBuffer);
                Map<String, List<String>> responseHeaders = getResponseHeaders(xhr.getAllResponseHeaders());
                continueDispatching(new GExternalHttpResponse(xhr.getResponseHeader("Content-Type"), bytes, responseHeaders, xhr.getStatus(), xhr.getStatusText()), result);
            }
        });
        return result.result;
    }

    private native void sendRequest(XMLHttpRequest request, ArrayBuffer body) /*-{
        request.send(body);
    }-*/;

    private ArrayBuffer bytesToArrayBuffer(byte[] bytes) {
        Uint8Array uint8Array = Uint8ArrayNative.create(bytes.length);
        for (int i = 0; i < uint8Array.length(); i++) {
            uint8Array.set(i, bytes[i]);
        }
        return uint8Array.buffer();
    }

    private byte[] arrayBufferToBytes(ArrayBuffer arrayBuffer) {
        Uint8Array uint8Array = Uint8ArrayNative.create(arrayBuffer);
        byte[] bytes = new byte[uint8Array.length()];
        for (int i = 0; i < uint8Array.length(); i++) {
            bytes[i] = (byte) uint8Array.get(i);
        }
        return bytes;
    }

    private Map<String, List<String>> getResponseHeaders(String allResponseHeaders) {
        Map<String, List<String>> responseHeaders = new HashMap<>();
        for(String responseHeader : allResponseHeaders.split("\n")) {
            int index = responseHeader.indexOf(":");
            if(index >= 0) {
                responseHeaders.put(responseHeader.substring(0, index), Collections.singletonList(responseHeader.substring(index + 1)));
            }
        }
        return responseHeaders;
    }

    private long lastCompletedRequest = -1L;
    private NativeHashMap<Long, FormContainer> asyncForms = new NativeHashMap<>();
    private NativeHashMap<Long, Pair<FormDockable, Integer>> asyncClosedForms = new NativeHashMap<>();
    private NativeHashMap<Long, Timer> openTimers = new NativeHashMap<>();

    public GAsyncFormController getAsyncFormController(long requestIndex) {
        return new GAsyncFormController() {
            @Override
            public FormContainer removeAsyncForm() {
                return asyncForms.remove(requestIndex);
            }

            @Override
            public void putAsyncForm(FormContainer container) {
                asyncForms.put(requestIndex, container);
            }

            @Override
            public Pair<FormDockable, Integer> removeAsyncClosedForm() {
                return asyncClosedForms.remove(requestIndex);
            }

            @Override
            public void putAsyncClosedForm(Pair<FormDockable, Integer> container) {
                asyncClosedForms.put(requestIndex, container);
            }

            public boolean checkNotCompleted() {
                return requestIndex > lastCompletedRequest;
            }

            @Override
            public boolean onServerInvocationOpenResponse() {
                lastCompletedRequest = requestIndex;
                return asyncForms.containsKey(requestIndex);
            }

            public boolean onServerInvocationCloseResponse() {
                lastCompletedRequest = requestIndex;
                return asyncClosedForms.containsKey(requestIndex);
            }

            @Override
            public boolean canShowDockedModal() {
                return GwtActionDispatcher.this.canShowDockedModal();
            }

            @Override
            public long getEditRequestIndex() {
                return requestIndex;
            }

            @Override
            public GwtActionDispatcher getDispatcher() {
                return GwtActionDispatcher.this;
            }

            @Override
            public void scheduleOpen(Scheduler.ScheduledCommand command) {
                Timer openFormTimer = new Timer() {
                    @Override
                    public void run() {
                        Scheduler.get().scheduleDeferred(() -> {
                            if (openTimers.remove(requestIndex) != null) {
                                if(checkNotCompleted())  //request is not completed yet
                                    command.execute();
                            } else
                                assert !checkNotCompleted();
                        });
                    }
                };
                openFormTimer.schedule(100);
                openTimers.put(requestIndex, openFormTimer);
            }

            @Override
            public void cancelScheduledOpening() {
                Timer timer = openTimers.remove(requestIndex);
                if(timer != null)
                    timer.cancel();
            }
        };
    }
}
