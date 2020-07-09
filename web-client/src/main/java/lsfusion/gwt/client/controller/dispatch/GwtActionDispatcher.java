package lsfusion.gwt.client.controller.dispatch;

import com.google.gwt.media.client.Audio;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.client.action.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.exception.ErrorHandlingCallback;
import lsfusion.gwt.client.base.exception.GExceptionManager;
import lsfusion.gwt.client.base.log.GLog;
import lsfusion.gwt.client.base.view.DialogBoxHelper;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.navigator.LogClientExceptionAction;

public abstract class GwtActionDispatcher implements GActionDispatcher {
    private boolean dispatchingPaused;

    private ServerResponseResult currentResponse = null;
    Object[] currentActionResults = null;
    private int currentActionIndex = -1;
    private int currentContinueIndex = -1;

    public void dispatchResponse(ServerResponseResult response) {
        dispatchResponse(response, -1);
    }
    public void dispatchResponse(ServerResponseResult response, int continueIndex) {
        assert response != null;

        Object[] actionResults = null;
        Throwable actionThrowable = null;
        GAction[] actions = response.actions;
        if (actions != null) {
            int beginIndex;
            if (dispatchingPaused) {
                beginIndex = currentActionIndex + 1;
                actionResults = currentActionResults;
                continueIndex = currentContinueIndex;

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
                    currentContinueIndex = continueIndex;
                    return;
                }

                actionResults[i] = dispatchResult;
            }
        }

        if (response.resumeInvocation) {
            continueIndex++;

            final int fContinueIndex = continueIndex;
            ErrorHandlingCallback<ServerResponseResult> continueRequestCallback =
                    new ErrorHandlingCallback<ServerResponseResult>() {
                        @Override
                        public void success(ServerResponseResult response) {
                            dispatchResponse(response, fContinueIndex);
                        }
                    };
            if (actionThrowable == null) {
                continueServerInvocation(response.requestIndex, actionResults, continueIndex, continueRequestCallback);
            } else {
                throwInServerInvocation(response.requestIndex, LogClientExceptionAction.fromWebClientToWebServer(actionThrowable), continueIndex, continueRequestCallback);
            }
        } else {
            if (actionThrowable != null)
                throw GExceptionManager.propagate(actionThrowable);
            postDispatchResponse(response);
        }
    }

    protected void postDispatchResponse(ServerResponseResult response) {
        assert !response.resumeInvocation;
    }

    protected abstract void throwInServerInvocation(long requestIndex, Throwable t, int continueIndex, AsyncCallback<ServerResponseResult> callback);

    protected abstract void continueServerInvocation(long requestIndex, Object[] actionResults, int continueIndex, AsyncCallback<ServerResponseResult> callback);

    protected final void pauseDispatching() {
        dispatchingPaused = true;
    }

    public void continueDispatching() {
        continueDispatching(null);
    }

    public void continueDispatching(Object currentActionResult) {
        if (currentActionResults != null && currentActionIndex >= 0) {
            currentActionResults[currentActionIndex] = currentActionResult;
        }
        if(currentResponse == null)
            GExceptionManager.throwStackedException("CURRENT RESPONSE IS NULL");
        dispatchResponse(currentResponse);
    }

    @Override
    public void execute(GFormAction action) {
    }

    @Override
    public void execute(GReportAction action) {
        GwtClientUtils.downloadFile(action.reportFileName, "lsfReport", action.reportExtension);
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
    public void execute(GOpenFileAction action) {
        GwtClientUtils.downloadFile(action.fileName, action.displayName, action.extension);
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
}
