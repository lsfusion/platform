package lsfusion.gwt.client.controller.dispatch;

import com.google.gwt.core.client.*;
import com.google.gwt.json.client.*;
import com.google.gwt.media.client.Audio;
import com.google.gwt.typedarrays.client.Uint8ArrayNative;
import com.google.gwt.typedarrays.shared.ArrayBuffer;
import com.google.gwt.typedarrays.shared.Uint8Array;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.xhr.client.XMLHttpRequest;
import lsfusion.gwt.client.action.*;
import lsfusion.gwt.client.action.com.GWriteToComPortAction;
import lsfusion.gwt.client.action.file.*;
import lsfusion.gwt.client.action.net.*;
import lsfusion.gwt.client.action.printer.GGetAvailablePrintersAction;
import lsfusion.gwt.client.action.printer.GPrintFileAction;
import lsfusion.gwt.client.action.printer.GWriteToPrinterAction;
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
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.dispatch.ExceptionResult;
import lsfusion.gwt.client.form.object.table.grid.view.GSimpleStateTableView;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.classes.GDateTimeDTO;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.form.view.FormDockable;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.navigator.window.GModalityShowFormType;
import lsfusion.gwt.client.view.MainFrame;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static lsfusion.gwt.client.base.GwtClientUtils.*;
import static lsfusion.gwt.client.controller.remote.action.PriorityErrorHandlingCallback.showErrorMessage;

// WARNING — DO NOT return a raw byte[] from any GAction.execute() in this
// dispatcher (or anywhere else whose result lands in a polymorphic Serializable
// field like ContinueNavigatorAction.actionResult). GWT 2.10's
// ServerSerializationStreamReader silently mis-handles a byte[] stuck into a
// polymorphic Serializable: the array elements aren't consumed from the token
// stream, the next string-field read picks up bytes as a string index, and the
// whole /main/dispatch request fails deserialization with
// ArrayIndexOutOfBoundsException at line 730 of getString — the client only
// sees a generic 500. Always wrap byte payloads in a tiny holder class (see
// GScreenShotResult / ScreenShotClientResult, or GExternalHttpResponse for the
// precedent); class-level field deserialization reads the array correctly
// because the expected type is byte[] rather than Serializable.
public abstract class GwtActionDispatcher implements GActionDispatcher {
    private boolean dispatchingPaused;

    protected ServerResponseResult currentResponse = null;
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

        dispatchResponse(response, null, null, continueIndex, onDispatchFinished, onRequestFinished);
    }
    public void continueDispatchResponse(Object actionResult, Throwable actionThrowable) {
        assert dispatchingPaused;
        dispatchResponse(null, actionResult, actionThrowable, -1, null, null);
    }

    // only the last action can be paused / continued with the result (and only it can return the result)
    public void dispatchResponse(ServerResponseResult response, Object actionResult, Throwable actionThrowable, int continueIndex, Runnable onDispatchFinished, Runnable onRequestFinished) {
        int beginIndex = 0;
        if (dispatchingPaused) { // continueDispatching
            beginIndex = currentActionIndex + 1;
            continueIndex = currentContinueIndex;
            response = currentResponse;
            onDispatchFinished = currentOnDispatchFinished;
            onRequestFinished = currentOnRequestFinished;

            assert beginIndex == response.actions.length;

            currentActionIndex = -1;
            currentContinueIndex = -1;
            currentResponse = null;
            currentOnDispatchFinished = null;
            currentOnRequestFinished = null;
            dispatchingPaused = false;
        }

        for (int i = beginIndex; i < response.actions.length; i++) {
            assert actionResult == null;

            GAction action = response.actions[i];
            try {
                dispatchingIndex = response.requestIndex;
                try {
                    GAction[] actions = response.actions;
                    int fI = i;
                    GActionDispatcherLookAhead lookAhead = new GActionDispatcherLookAhead() {
                        int index = fI;

                        @Override
                        public GAction next() {
                            index++; while(index < actions.length && actions[index] == null) index++;
                            if(index >= actions.length)
                                return null;

                            return actions[index];
                        }

                        @Override
                        public void drop() {
                            actions[index] = null;
                        }
                    };

                    //for unsupported actions null is send to preserve number of actions and thus the order of responses
                    actionResult = action == null ? null : action.dispatch(this, lookAhead);
                } finally {
                    dispatchingIndex = -1;
                }
            } catch (Throwable ex) {
                actionThrowable = ex;
                break;
            }

            // actionResult can be not null only if not dispatchingPaused and it's the last action
            assert actionResult == null || (!dispatchingPaused && i == response.actions.length - 1);

            if (dispatchingPaused) {
                assert i == response.actions.length - 1;
                currentResponse = response;
                currentActionIndex = i;
                currentContinueIndex = continueIndex;
                currentOnDispatchFinished = onDispatchFinished;
                currentOnRequestFinished = onRequestFinished;
                return;
            }
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
                continueServerInvocation(response.requestIndex, actionResult, continueIndex, continueRequestCallback);
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

    protected abstract void continueServerInvocation(long requestIndex, Object actionResult, int continueIndex, RequestAsyncCallback<ServerResponseResult> callback);

    public abstract void executeVoidAction(long requestIndex);

    // synchronization is guaranteed pretty tricky
    // in RemoteDispatchAsync there is a linked list q of all executing actions, where all responses are queued, and all continue invoications are put into it's beginning
    protected final void pauseDispatching() {
        dispatchingPaused = true;
    }

    protected long dispatchingIndex = -1;
    public long getDispatchingIndex() {
        if (currentResponse == null) // means that we continueDispatching before exiting to dispatchResponse cycle (for example LogicalCellRenderer commits editing immediately)
            return dispatchingIndex;
        else
            return currentResponse.requestIndex;
    }

    public <T> void continueDispatching(T actionResult, Throwable actionThrowable, Result<T> result) {
        assert dispatchingPaused;
        if (currentResponse == null) { // means that we continueDispatching before exiting to dispatchResponse cycle (for example LogicalCellRenderer commits editing immediately)
            // in this case we have to return result as no pauseDispatching happened
            // however scheduleDeferred might be used
            dispatchingPaused = false;

            if(actionThrowable != null)
                throw GExceptionManager.propagate(actionThrowable);

            if(result != null)
                result.set(actionResult);
        } else {
            continueDispatchResponse(actionResult, actionThrowable);
        }
    }

    protected abstract PopupOwner getPopupOwner();

    public boolean canShowDockedModal() {
        return true;
    }

    protected abstract FormsController.OpenContext getOpenContext(GFormAction action);
    protected abstract FormsController getFormsController();

    @Override
    public void execute(GFormAction action) {
        executeAsyncNoResult(action.showFormType.isModal() && action.syncType, onResult -> {
            getFormsController().openForm(getAsyncFormController(getDispatchingIndex()), action.form, action.showFormType, action.forbidDuplicate, action.syncType, action.formId, getOpenContext(action), canShowDockedModal(), onResult);
        });
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
            executeAsyncNoResult(action.syncType, onResult -> {
                DialogBoxHelper.showMessageBox(action.caption, GLog.toPrintMessage(message, image, action.data, action.titles), backgroundClass, getPopupOwner(), chosenOption -> onResult.accept(null));
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
    public Integer execute(GConfirmAction action) {
        return executeAsyncResult(onResult -> {
            DialogBoxHelper.showConfirmBox(action.caption, EscapeUtils.toHTML(action.message, StaticImage.MESSAGE_WARN), action.cancel, action.timeout, action.initialValue, getPopupOwner(),
                    chosenOption -> onResult.accept(chosenOption.asInteger(), null));
        });
    }

    @Override
    public void execute(GHideFormAction action, GActionDispatcherLookAhead lookAhead) {
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
    public void execute(GControllerResultAction action) {
    }

    @Override
    public void execute(GControllerExceptionAction action) {
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
    public GReadResult execute(GReadAction action) {
        return executeAsyncResultFlutter("readFile", new String[] {action.sourcePath}, res -> {
            String errorValue = getJSONError(res);
            return errorValue != null ?
                    new GReadResult(errorValue, null, null, null) :
                    new GReadResult(null, getJSONStringResult(res), getFileName(action.sourcePath), getFileExtension(action.sourcePath));
        });
    }

    @Override
    public String execute(GDeleteFileAction action) {
        return executeAsyncResultFlutter("deleteFile", new String[] {action.source}, this::getJSONStringResult);
    }

    protected <T> T executeAsyncResult(Consumer<BiConsumer<T, Throwable>> run) {
        pauseDispatching();

        Result<T> result = new Result<>();
        BiConsumer<T, Throwable> onResult = (res, actionThrowable) -> continueDispatching(res, actionThrowable, result);
        try {
            run.accept(onResult);
        }  catch (Throwable e) {
            onResult.accept(null, e);
            throw e;
        }

        return result.result;
    }

    protected void executeAsyncNoResult(boolean sync, Consumer<Consumer<Throwable>> run) {
        if(sync) {
            Object result = executeAsyncResult(onResult -> run.accept(throwable -> onResult.accept(null, throwable)));
            assert result == null;
        } else
            run.accept(throwable -> {});
    }

    private <T> T executeAsyncResultFlutter(String command, Object[] arguments, Function<JavaScriptObject, T> getResult) {
        JavaScriptObject flutter = getFlutterObject();
        if (flutter != null) {
            return executeAsyncResult(onResult -> executeFlutter(flutter, command, arguments, res -> onResult.accept(getResult.apply(res), null)));
        } else {
            throw new UnsupportedOperationException(command + " is supported only in flutter client");
        }
    }

    private static void executeNoResultFlutter(String command, Object[] arguments, Runnable noFlutter) {
        JavaScriptObject flutter = getFlutterObject();
        if (flutter != null) {
            executeFlutter(flutter, command, arguments, res -> {});
        } else {
            if(noFlutter == null)
                throw new UnsupportedOperationException(command + " is supported only in flutter client");

            noFlutter.run();
        }
    }

    @Override
    public boolean execute(GFileExistsAction action) {
        return executeAsyncResultFlutter("fileExists", new String[]{action.source}, this::getJSONBooleanResult);
    }

    @Override
    public String execute(GMkDirAction action) {
        return executeAsyncResultFlutter("makeDir", new String[] {action.source}, this::getJSONStringResult);
    }

    @Override
    public String execute(GMoveFileAction action) {
        return executeAsyncResultFlutter("moveFile", new String[] {action.source, action.destination}, this::getJSONStringResult);
    }

    @Override
    public String execute(GCopyFileAction action) {
        return executeAsyncResultFlutter("copyFile", new String[] {action.source, action.destination}, this::getJSONStringResult);
    }

    @Override
    public GListFilesResult execute(GListFilesAction action) {
        return executeAsyncResultFlutter("listFiles", new Object[] {action.source, action.recursive}, res -> getListFilesResult(new JSONObject(res).get("result")));
    }

    private GListFilesResult getListFilesResult(JSONValue res) {
        try {
            JSONArray result =res.isArray();
            String[] namesArray = new String[result.size()];
            Boolean[] dirsArray = new Boolean[result.size()];
            GDateTimeDTO[] modifiedArray = new GDateTimeDTO[result.size()];
            Long[] sizesArray = new Long[result.size()];

            for (int i = 0; i < result.size(); i++) {
                JSONObject entry = result.get(i).isObject();
                namesArray[i] = entry.get("path").isString().stringValue();
                dirsArray[i] = entry.get("isDirectory").isBoolean().booleanValue() ? true : null;
                modifiedArray[i] = GDateTimeDTO.fromJsDate(JsDate.create(entry.get("modifiedDateTime").isString().stringValue()));
                sizesArray[i] = (long) entry.get("fileSize").isNumber().doubleValue();
            }
            return new GListFilesResult(null, namesArray, dirsArray, modifiedArray, sizesArray);
        } catch (Exception e) {
            return new GListFilesResult(e.getMessage(), null, null, null, null);
        }
    }

    @Override
    public void execute(GWriteAction action) {
        if (action.fileUrl != null) {
            String downloadURL = getAppDownloadURL(action.fileUrl);
            //todo: status 401 from RestAuthenticationEntryPoint
            executeNoResultFlutter("writeFile", new Object[]{getFullUrl(downloadURL), action.filePath}, () -> fileDownload(downloadURL));
        }
    }

    @Override
    public GRunCommandActionResult execute(GRunCommandAction action) {
        return executeAsyncResultFlutter("runCommand", new String[]{action.command}, res -> new GRunCommandActionResult(getJSONString(res, "cmdOut"), getJSONString(res, "cmdErr"), getJSONInt(res, "exitValue")));
    }

    @Override
    public String execute(GGetAvailablePrintersAction action) {
        return executeAsyncResultFlutter("getAvailablePrinters", new String[] {}, this::getJSONStringResult);
    }

    @Override
    public String execute(GPrintFileAction action) {
        return executeAsyncResultFlutter("print", new String[] {action.fileData, action.filePath, null, action.printerName}, this::getJSONStringResult);
    }

    @Override
    public String execute(GWriteToPrinterAction action) {
        return executeAsyncResultFlutter("print", new String[] {null, null, action.text, action.printerName}, this::getJSONStringResult);
    }

    @Override
    public String execute(GTcpAction action) {
        return executeAsyncResultFlutter("sendTCP", new Object[] {action.host, action.port, action.fileBytes, nvl(action.timeout, 3600000)}, this::getJSONStringResult);
    }

    @Override
    public void execute(GUdpAction action) {
        executeNoResultFlutter("sendUDP", new Object[]{action.host, action.port, action.fileBytes}, null);
    }

    @Override
    public void execute(GWriteToSocketAction action) {
        executeNoResultFlutter("writeToSocket", new Object[]{action.ip, action.port, action.text, action.charset}, null);
    }

    @Override
    public String execute(GPingAction action) {
        return executeAsyncResultFlutter("ping", new String[] {action.host}, this::getJSONStringResult);
    }

    private boolean getJSONBooleanResult(JavaScriptObject res) {
        Boolean exists = getJSONBoolean(res, "result");
        return exists != null && exists;
    }

    private String getJSONStringResult(JavaScriptObject res) {
        return getJSONString(res, "result");
    }

    private String getJSONError(JavaScriptObject res) {
        return getJSONString(res, "error");
    }

    private String getJSONString(JavaScriptObject res, String key) {
        JSONValue json = new JSONObject(res).get(key);
        if(json != null) {
            return json.isNull() != null ? null : json.isString().stringValue();
        } else {
            return null;
        }
    }

    private int getJSONInt(JavaScriptObject res, String key) {
        JSONValue json = new JSONObject(res).get(key);
        if(json != null) {
            return json.isNull() != null ? 0 : (int) json.isNumber().doubleValue();
        } else {
            return 0;
        }
    }

    private Boolean getJSONBoolean(JavaScriptObject res, String key) {
        JSONValue json = new JSONObject(res).get(key);
        if(json != null) {
            return json.isNull() != null ? null : json.isBoolean().booleanValue();
        } else {
            return null;
        }
    }

    @Override
    public String execute(GWriteToComPortAction action) {
        return executeAsyncResultFlutter("writeToComPort", new Object[] {action.comPort, action.baudRate, action.file}, this::getJSONStringResult);
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
        private final List<BiConsumer<Object, Throwable>> actionOnResults = new ArrayList<>();

        public Object addAction(GClientWebAction action) {
            Consumer<BiConsumer<Object, Throwable>> run = onResult -> {
                actions.add(action);
                actionOnResults.add(onResult);

                flush();
            };

            if(action.syncType)
                return executeAsyncResult(run);

            run.accept((actionResult, throwable) -> {});
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
            onActionExecuted(action, null, null);
        }

        private void onActionExecuted(GClientWebAction action, Object actionResult, Throwable actionThrowable) {
            isExecuting = false;
            GClientWebAction remove = actions.remove(0);
            assert remove == action;
            BiConsumer<Object, Throwable> onResult = actionOnResults.remove(0);
            onResult.accept(actionResult, actionThrowable);

            flush();
        }

        private native JavaScriptObject getOnResult(GClientWebAction action) /*-{
            var thisObj = this;
            return function (value) {
                return thisObj.@JSExecutor::onJSFunctionExecuted(*)(action, value);
            }
        }-*/;

        private native JavaScriptObject getOnException(GClientWebAction action) /*-{
            var thisObj = this;
            return function (value) {
                return thisObj.@JSExecutor::onJSFunctionException(*)(action, value);
            }
        }-*/;

        private JavaScriptObject getController() {
            return GwtActionDispatcher.this.getController();
        }
        private void onJSFunctionExecuted(GClientWebAction action, JavaScriptObject result) {
            onActionExecuted(action, PValue.convertFileValueBack(GSimpleStateTableView.convertFromJSValue(action.returnType, result)), null);
        }
        private void onJSFunctionException(GClientWebAction action, String message) {
            onActionExecuted(action, null, new RuntimeException(message));
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
                    if(fncParams > arguments.length()) // takes onException
                        arguments.push(getOnException(action));
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
    public GScreenShotResult execute(GScreenShotAction action) {
        if (action.containerSID != null)
            throw new UnsupportedOperationException("SCREENSHOT of a container or form is supported only inside form context");
        return screenShotElement(action.html, com.google.gwt.dom.client.Document.get().getBody());
    }

    protected GScreenShotResult screenShotElement(boolean html, com.google.gwt.dom.client.Element element) {
        // SCREENSHOT HTML returns the element's children markup (innerHTML), not
        // the element's outer tag. Matches the typical "capture the content of
        // this container" intent rather than the wrapper itself.
        if (html)
            return new GScreenShotResult(stringToUtf8Bytes(element.getInnerHTML()));
        return captureElementToPng(element);
    }

    private GScreenShotResult captureElementToPng(com.google.gwt.dom.client.Element element) {
        return executeAsyncResult(onResult -> runHtmlToImage(element, new ScreenShotCallback() {
            @Override
            public void onBuffer(ArrayBuffer buffer) {
                onResult.accept(new GScreenShotResult(arrayBufferToBytes(buffer)), null);
            }
            @Override
            public void onError(String error) {
                onResult.accept(null, new RuntimeException(error));
            }
        }));
    }

    private native void runHtmlToImage(com.google.gwt.dom.client.Element element, ScreenShotCallback callback) /*-{
        // Wait for webfonts (icons) to load before serializing the DOM into the SVG
        // foreignObject; otherwise icon glyphs render as empty boxes.
        var ready = ($wnd.document && $wnd.document.fonts && $wnd.document.fonts.ready)
            ? $wnd.document.fonts.ready
            : $wnd.Promise.resolve();
        // Build font-embed CSS from every accessible @font-face rule in the page.
        // html-to-image's default getFontEmbedCSS only includes fonts referenced
        // directly on elements; fonts that drive icon glyphs through ::before
        // pseudos (bootstrap-icons) get dropped, leaving icon boxes empty.
        function buildAllFontsCSS() {
            var rules = [];
            for (var i = 0; i < $wnd.document.styleSheets.length; i++) {
                var sheet = $wnd.document.styleSheets[i];
                var cssRules = null;
                try { cssRules = sheet.cssRules; } catch (e) { continue; }
                if (!cssRules) continue;
                for (var j = 0; j < cssRules.length; j++) {
                    var rule = cssRules[j];
                    if (rule.type === 5) { // CSSRule.FONT_FACE_RULE
                        rules.push({text: rule.cssText, sheetHref: sheet.href});
                    }
                }
            }
            // For each rule, replace url(relative) with absolute, then fetch and inline as data URI.
            return $wnd.Promise.all(rules.map(function(r) {
                return inlineRuleFontUrls(r.text, r.sheetHref);
            })).then(function(parts) {
                return parts.join("\n");
            });
        }
        function inlineRuleFontUrls(cssText, baseHref) {
            // Match url("..."), url('...'), url(...) — skip data: URIs.
            var urlRe = /url\(\s*(['"]?)([^'")]+)\1\s*\)/g;
            var matches = [];
            var m;
            while ((m = urlRe.exec(cssText)) !== null) {
                if (m[2].indexOf("data:") !== 0) matches.push(m[2]);
            }
            return $wnd.Promise.all(matches.map(function(u) {
                var abs;
                try { abs = new $wnd.URL(u, baseHref || $wnd.location.href).href; } catch (_) { abs = u; }
                return $wnd.fetch(abs).then(function(r){return r.ok ? r.blob() : null;}).then(function(b){
                    if (!b) return {orig: u, data: null};
                    return new $wnd.Promise(function(resolve) {
                        var fr = new $wnd.FileReader();
                        fr.onload = function() { resolve({orig: u, data: fr.result}); };
                        fr.onerror = function() { resolve({orig: u, data: null}); };
                        fr.readAsDataURL(b);
                    });
                }).then(null, function(){ return {orig: u, data: null}; });
            })).then(function(results) {
                var out = cssText;
                results.forEach(function(r) {
                    if (r.data) {
                        // Escape special chars in original URL for regex
                        var esc = r.orig.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
                        out = out.replace(new $wnd.RegExp("url\\(\\s*['\"]?" + esc + "['\"]?\\s*\\)", 'g'), 'url("' + r.data + '")');
                    }
                });
                return out;
            });
        }
        // Fractional width rounding inside the SVG foreignObject layout pass
        // can make flex-wrap kick in at the boundary case where it doesn't in
        // the native render. Two pathologies hit:
        //   (a) inline-flex btn-group with active flex-shrink — foreignObject's
        //       shrink redistribution overshoots and the last child wraps onto
        //       a hidden 2nd row;
        //   (b) plain flex-wrap row that fits comfortably in live — sub-pixel
        //       position drift pushes one child past the wrap boundary and it
        //       lands on a hidden 2nd row of the flex container (the parent
        //       crops to one row, so the child visually disappears).
        // Fix: walk the captured subtree, find every element that uses
        // flex-wrap AND is currently a single row in the live DOM AND has
        // less than 1 px of slack between its content-box width and its
        // children's combined outer widths (the only cases where sub-pixel
        // rounding can flip wrap on). For each, pin BOTH:
        //   min-width = ceil(rect.width) + 1  // covers (b)
        //   flex-wrap: nowrap                 // covers (a)
        // Either alone fixes only one of the two pathologies; together they
        // close both with no visible disruption to other layout.
        // Inline styles are restored after capture so the live DOM is untouched.
        var pinnedWrap = [];
        function isSingleRow(el) {
            var kids = el.children;
            if (!kids || kids.length < 2) return true;
            var firstTop = kids[0].offsetTop;
            for (var k = 1; k < kids.length; k++) {
                if (Math.abs(kids[k].offsetTop - firstTop) > 0.5) return false;
            }
            return true;
        }
        function sumChildrenOuter(el) {
            var sum = 0;
            for (var i = 0; i < el.children.length; i++) {
                var k = el.children[i];
                var r = k.getBoundingClientRect();
                var cs = $wnd.getComputedStyle(k);
                var ml = parseFloat(cs.marginLeft) || 0;
                var mr = parseFloat(cs.marginRight) || 0;
                sum += r.width + ml + mr;
            }
            return sum;
        }
        function pinWrapElements(root) {
            var all = root.querySelectorAll("*");
            var list = [root];
            for (var i = 0; i < all.length; i++) list.push(all[i]);
            for (var j = 0; j < list.length; j++) {
                var n = list[j];
                if (n.nodeType !== 1) continue;
                var cs = $wnd.getComputedStyle(n);
                var fw = cs.flexWrap;
                if (fw !== "wrap" && fw !== "wrap-reverse") continue;
                if (!n.children || n.children.length === 0) continue;
                if (!isSingleRow(n)) continue;
                var rect = n.getBoundingClientRect();
                var bl = parseFloat(cs.borderLeftWidth) || 0;
                var br = parseFloat(cs.borderRightWidth) || 0;
                var pl = parseFloat(cs.paddingLeft) || 0;
                var pr = parseFloat(cs.paddingRight) || 0;
                var content = rect.width - bl - br - pl - pr;
                var kidsSum = sumChildrenOuter(n);
                // Slack < 1 px between content-box and children's outer widths
                // means sub-pixel rounding in foreignObject is the only thing
                // keeping wrap off. Pin both min-width (covers the
                // sub-pixel-position case — children land past the wrap
                // boundary and drop to a hidden 2nd row) AND flex-wrap:nowrap
                // (covers the shrink-active case — flex-shrink redistribution
                // overshoots by more than 1 px and wraps despite the +1 px
                // slack). Either alone fixes only one of the two pathologies.
                if (content - kidsSum >= 1.0) continue;
                pinnedWrap.push({el: n, prevMW: n.style.minWidth, prevFW: n.style.flexWrap});
                n.style.minWidth = (Math.ceil(rect.width) + 1) + "px";
                n.style.flexWrap = "nowrap";
            }
        }
        function restoreWrapPin() {
            for (var i = 0; i < pinnedWrap.length; i++) {
                var p = pinnedWrap[i];
                if (p.prevMW) p.el.style.minWidth = p.prevMW;
                else p.el.style.removeProperty("min-width");
                if (p.prevFW) p.el.style.flexWrap = p.prevFW;
                else p.el.style.removeProperty("flex-wrap");
            }
            pinnedWrap.length = 0;
        }
        pinWrapElements(element);

        ready.then(function() {
            // Cache the assembled CSS — fonts don't change at runtime, and the
            // per-call cost is ~6 woff2 fetches + base64 (slow on first paint).
            return $wnd.__lsfRenderFontCSS || ($wnd.__lsfRenderFontCSS = buildAllFontsCSS());
        }).then(function(embedCSS) {
            return $wnd.htmlToImage.toBlob(element, {cacheBust: false, pixelRatio: 1, fontEmbedCSS: embedCSS, skipFonts: false});
        }).then(function(blob) {
            restoreWrapPin();
            if (blob == null) {
                callback.@lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher.ScreenShotCallback::onError(Ljava/lang/String;)("html-to-image: empty blob");
                return;
            }
            var reader = new FileReader();
            reader.onload = function() {
                callback.@lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher.ScreenShotCallback::onBuffer(Lcom/google/gwt/typedarrays/shared/ArrayBuffer;)(reader.result);
            };
            reader.onerror = function() {
                callback.@lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher.ScreenShotCallback::onError(Ljava/lang/String;)("html-to-image: read failure");
            };
            reader.readAsArrayBuffer(blob);
        }, function(err) {
            restoreWrapPin();
            callback.@lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher.ScreenShotCallback::onError(Ljava/lang/String;)("html-to-image: " + (err && err.message ? err.message : err));
        });
    }-*/;

    private static byte[] stringToUtf8Bytes(String s) {
        if (s == null) return new byte[0];
        com.google.gwt.typedarrays.shared.Uint8Array uint8 = encodeUtf8(s);
        byte[] bytes = new byte[uint8.length()];
        for (int i = 0; i < bytes.length; i++)
            bytes[i] = (byte) uint8.get(i);
        return bytes;
    }

    private static native com.google.gwt.typedarrays.shared.Uint8Array encodeUtf8(String s) /*-{
        if ($wnd.TextEncoder)
            return new $wnd.TextEncoder().encode(s);
        // fallback for legacy browsers
        var bin = unescape(encodeURIComponent(s));
        var arr = new Uint8Array(bin.length);
        for (var i = 0; i < bin.length; i++) arr[i] = bin.charCodeAt(i);
        return arr;
    }-*/;

    private interface ScreenShotCallback {
        void onBuffer(ArrayBuffer buffer);
        void onError(String error);
    }

    @Override
    public Object execute(GHttpClientAction action) {
        return executeAsyncResult(onResult -> {
            XMLHttpRequest request = XMLHttpRequest.create();
            request.open(action.method.name(), action.connectionString);
            request.setResponseType("arraybuffer");
            for (Map.Entry<String, String> header : action.headers.entrySet()) {
                request.setRequestHeader(header.getKey(), header.getValue());
            }
            sendRequest(request, action.body != null ? bytesToArrayBuffer(action.body) : null);

            request.setOnReadyStateChange(xhr -> {
                if (xhr.getReadyState() == XMLHttpRequest.DONE) {
                    ArrayBuffer arrayBuffer = xhr.getResponseArrayBuffer();
                    byte[] bytes = arrayBufferToBytes(arrayBuffer);
                    Map<String, List<String>> responseHeaders = getResponseHeaders(xhr.getAllResponseHeaders());
                    onResult.accept(new GExternalHttpResponse(xhr.getResponseHeader("Content-Type"), bytes, responseHeaders, xhr.getStatus(), xhr.getStatusText()), null);
                }
            });
        });
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
