package lsfusion.gwt.form.client.form.dispatch;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.base.client.ErrorHandlingCallback;
import lsfusion.gwt.base.client.GwtClientUtils;
import lsfusion.gwt.base.client.ui.DialogBoxHelper;
import lsfusion.gwt.form.client.log.GLog;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.form.shared.view.actions.*;

public abstract class GwtActionDispatcher implements GActionDispatcher {
    private boolean dispatchingPaused;

    private ServerResponseResult currentResponse = null;
    Object[] currentActionResults = null;
    private int currentActionIndex = -1;

    private final ErrorHandlingCallback<ServerResponseResult> continueRequestCallback =
            new ErrorHandlingCallback<ServerResponseResult>() {
                @Override
                public void success(ServerResponseResult response) {
                    dispatchResponse(response);
                }
            };

    public void dispatchResponse(ServerResponseResult response) {
        assert response != null;

        try {
            Object[] actionResults = null;
            Throwable actionThrowable = null;
            GAction[] actions = response.actions;
            if (actions != null) {
                int beginIndex;
                if (dispatchingPaused) {
                    beginIndex = currentActionIndex + 1;
                    actionResults = currentActionResults;

                    currentActionIndex = -1;
                    currentActionResults = null;
                    currentResponse = null;
                    dispatchingPaused = false;
                } else {
                    beginIndex = 0;
                    actionResults = new Object[actions.length];
                }

                for (int i = beginIndex; i < actions.length; i++) {
                    GAction action = actions[i];
                    Object dispatchResult;
                    try {
                        //для неподдерживаемых action'ов присылается null-ссылка, чтобы сохранить порядок результатов выполнения action'ов
                        dispatchResult = action == null ? null : action.dispatch(this);
                    } catch (Throwable ex) {
                        actionThrowable = ex;
                        break;
                    }

                    if (dispatchingPaused) {
                        currentResponse = response;
                        currentActionResults = actionResults;
                        currentActionIndex = i;
                        return;
                    }

                    actionResults[i] = dispatchResult;
                }
            }

            if (response.resumeInvocation) {
                if (actionThrowable == null) {
                    continueServerInvocation(actionResults, continueRequestCallback);
                } else {
                    throwInServerInvocation(actionThrowable, continueRequestCallback);
                }
            } else {
                if (actionThrowable != null) {
                    throw new RuntimeException(actionThrowable);
                }
                postDispatchResponse(response);
            }
        } catch (Exception e) {
            handleDispatchException(e);
        }
    }

    protected void postDispatchResponse(ServerResponseResult response) {
        assert !response.resumeInvocation;
    }

    protected void handleDispatchException(Throwable t) {
        GWT.log("Error dispatching ServerResponseResult: ", t);
        Log.error("Error dispatching ServerResponseResult: ", t);
        DialogBoxHelper.showMessageBox(true, "Error", t.getMessage(), null);
    }

    protected abstract void throwInServerInvocation(Throwable t, AsyncCallback<ServerResponseResult> callback);

    protected abstract void continueServerInvocation(Object[] actionResults, AsyncCallback<ServerResponseResult> callback);

    protected final void pauseDispatching() {
        dispatchingPaused = true;
    }

    public void continueDispatching() {
        continueDispatching(null);
    }

    public void continueDispatching(Object currentActionResult) {
        currentActionResults[currentActionIndex] = currentActionResult;
        dispatchResponse(currentResponse);
    }

    @Override
    public void execute(GFormAction action) {
    }

    @Override
    public void execute(GReportAction action) {
    }

    @Override
    public void execute(GRunOpenReportAction action) {
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
    public int execute(GConfirmAction action) {
        return 0;
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
    public void execute(GFocusAction action) {
    }

    @Override
    public void execute(GOpenFileAction action) {
        downloadFile(action.filePath);
    }

    @Override
    public void execute(GExportFileAction action) {
        if (action.filePaths != null) {
            for (String filePath : action.filePaths) {
                downloadFile(filePath);
            }
        }
    }

    private void downloadFile(String filePath) {
        if (filePath != null) {
            String fileUrl = GwtClientUtils.getWebAppBaseURL() + "downloadFile?name=" + filePath;
            Window.open(fileUrl, filePath, "");
        }
    }

    @Override
    public void execute(GActivateTabAction action) {
    }
}
