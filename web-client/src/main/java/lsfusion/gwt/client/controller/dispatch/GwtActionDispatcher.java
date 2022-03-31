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
import com.google.gwt.xhr.client.XMLHttpRequest;
import lsfusion.gwt.client.action.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Result;
import lsfusion.gwt.client.base.exception.GExceptionManager;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.log.GLog;
import lsfusion.gwt.client.base.view.DialogBoxHelper;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.controller.remote.action.RequestAsyncCallback;
import lsfusion.gwt.client.controller.remote.action.RequestCountingErrorHandlingCallback;
import lsfusion.gwt.client.controller.remote.action.RequestErrorHandlingCallback;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.navigator.LogClientExceptionAction;
import lsfusion.gwt.client.form.object.table.grid.view.GSimpleStateTableView;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class GwtActionDispatcher implements GActionDispatcher {
    private boolean dispatchingPaused;

    protected ServerResponseResult currentResponse = null;
    Object[] currentActionResults = null;
    private int currentActionIndex = -1;
    private int currentContinueIndex = -1;
    private Runnable currentOnDispatchFinished = null;
    private Runnable currentOnRequestFinished = null;

    protected abstract void onServerInvocationResponse(ServerResponseResult response);

    public static abstract class ServerResponseCallback extends RequestCountingErrorHandlingCallback<ServerResponseResult> {

        private final boolean disableForbidDuplicate;

        public ServerResponseCallback(boolean disableForbidDuplicate) {
            this.disableForbidDuplicate = disableForbidDuplicate;
        }

        protected abstract GwtActionDispatcher getDispatcher();

        protected Runnable getOnRequestFinished() {
            return null;
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
        public void onFailure(Throwable caught) {
            Runnable onRequestFinished = getOnRequestFinished();
            if(onRequestFinished != null)
                onRequestFinished.run();

            super.onFailure(caught);
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
                        public void onFailure(Throwable caught) {
                            if(fOnRequestFinished != null)
                                fOnRequestFinished.run();
                            super.onFailure(caught);
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

    public boolean canShowDockedModal() {
        return true;
    }

    @Override
    public void execute(GFormAction action) {
    }

    @Override
    public void execute(GReportAction action) {
        GwtClientUtils.downloadFile(action.reportFileName, "lsfReport", action.reportExtension, action.autoPrint);
    }

    @Override
    public Object execute(GChooseClassAction action) {
        return null;
    }

    @Override
    public void execute(GMessageAction action) {
        pauseDispatching();
        DialogBoxHelper.showMessageBox(false, action.caption, action.message, new DialogBoxHelper.CloseCallback() {
            @Override
            public void closed(DialogBoxHelper.OptionType chosenOption) {
                continueDispatching();
            }
        });
    }

    @Override
    public Object execute(GConfirmAction action) {
        pauseDispatching();

        Result<Object> result = new Result<>();
        DialogBoxHelper.showConfirmBox(action.caption, action.message, action.cancel, action.timeout, action.initialValue,
                chosenOption -> continueDispatching(chosenOption.asInteger(), result));
        return result.result;
    }

    @Override
    public void execute(GLogMessageAction action) {
        if (action.failed) {
            GLog.error(action.message, action.data, action.titles);
        } else {
            GLog.message(action.message);
        }
    }

    @Override
    public void execute(GHideFormAction action) {
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
        Window.open(action.uri, "_blank", "");
    }

    @Override
    public void execute(GEditNotPerformedAction action) {
    }

    @Override
    public void execute(GOpenFileAction action) {
        GwtClientUtils.downloadFile(action.fileName, action.displayName, action.extension, false);
    }

    //todo: по идее, action должен заливать куда-то в сеть выбранный локально файл
    @Override
    public String execute(GLoadLinkAction action) {
        return null;
    }

    @Override
    public void execute(final GBeepAction action) {
        String fileUrl = GwtClientUtils.getDownloadURL(action.filePath, null, "wav", true);
        Audio beep = Audio.createIfSupported();
        if (beep != null) {
            beep.setSrc(fileUrl);
            beep.play();
        }
    }

    @Override
    public void execute(GActivateFormAction action) {
    }

    @Override
    public void execute(GMaximizeFormAction action) {
    }

    @Override
    public void execute(GChangeColorThemeAction action) {
    }

    @Override
    public void execute(GResetWindowsLayoutAction action) {
    }

    private class JSExecutor {
        private final List<GClientJSAction> actions = new ArrayList<>();

        public void addAction(GClientJSAction action) {
            if (action.syncType)
                pauseDispatching();

            if (action.resource != null) {
                actions.add(action);
                flush();
            } else {
                GwtClientUtils.consoleError("Resource load error: " + action.resourceName);
            }
        }

        private boolean isExecuting = false;
        private void flush() {
            if (!isExecuting && !actions.isEmpty()) {
                GClientJSAction action = actions.get(0);
                isExecuting = true;
                if (action.isFile)
                    executeFile(action);
                else
                    executeJSFunction(action);
            }
        }

        private native void executeFile(GClientJSAction action)/*-{
            var thisObj = this;
            var resourcePath = (@lsfusion.gwt.client.view.MainFrame::devMode ? 'dev' : 'static') + action.@GClientJSAction::resource;

            if (resourcePath.endsWith('js')) {
                var documentScripts = $wnd.document.scripts;
                for (var i = 0; i < documentScripts.length; i++) {
                    var src = documentScripts[i].src;
                    if (src != null && src.endsWith(resourcePath)) {
                        thisObj.@JSExecutor::onActionExecuted(*)(action);
                        return;
                    }
                }
                var scr = document.createElement('script');
                scr.src = resourcePath;
                scr.type = 'text/javascript';
                $wnd.document.head.appendChild(scr);
                scr.onload = function() {thisObj.@JSExecutor::onActionExecuted(*)(action); }
            } else if (resourcePath.endsWith('css')) {
                var documentStyleSheets = $wnd.document.styleSheets;
                for (var j = 0; j < documentStyleSheets.length; j++) {
                    var href = documentStyleSheets[j].href;
                    if (href != null && href.endsWith(resourcePath)) {
                        thisObj.@JSExecutor::onActionExecuted(*)(action);
                        return;
                    }
                }
                var link = document.createElement("link");
                link.href = resourcePath;
                link.type = "text/css";
                link.rel = "stylesheet";
                $wnd.document.head.appendChild(link);
                thisObj.@JSExecutor::onActionExecuted(*)(action);
            }
        }-*/;

        private void onActionExecuted(GClientJSAction action) {
            if (action.syncType)
                continueDispatching();

            isExecuting = false;
            actions.remove(action);
            flush();
        }

        private void executeJSFunction(GClientJSAction action) {
            JsArray<JavaScriptObject> arguments = JavaScriptObject.createArray().cast();
            ArrayList<Object> types = action.types;
            for (int i = 0; i < types.size(); i++) {
                arguments.push(GSimpleStateTableView.convertValue((GType) types.get(i), action.values.get(i)));
            }
            String function = action.resource;
            GwtClientUtils.call(GwtClientUtils.getGlobalField(function.substring(0, function.indexOf("("))), arguments);
            onActionExecuted(action);
        }
    }

    private JSExecutor jsExecutor;
    @Override
    public void execute(GClientJSAction action) {
        if (jsExecutor == null)
            jsExecutor = new JSExecutor();
        jsExecutor.addAction(action);
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

            public boolean checkNotCompleted() {
                return requestIndex > lastCompletedRequest;
            }

            @Override
            public boolean onServerInvocationResponse() {
                lastCompletedRequest = requestIndex;
                return asyncForms.containsKey(requestIndex);
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
