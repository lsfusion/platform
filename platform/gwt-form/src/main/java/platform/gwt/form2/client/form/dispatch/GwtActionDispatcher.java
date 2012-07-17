package platform.gwt.form2.client.form.dispatch;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import platform.gwt.base.client.ErrorAsyncCallback;
import platform.gwt.form2.client.form.ui.dialog.MessageBox;
import platform.gwt.form2.shared.actions.form.ServerResponseResult;
import platform.gwt.view2.actions.*;

public abstract class GwtActionDispatcher implements GActionDispatcher {
    /** c/p from java.swing.JOptionPane */
    public static final int YES_OPTION = 0;
    public static final int NO_OPTION = 1;

    private boolean dispatchingPaused;

    private ServerResponseResult currentResponse = null;
    Object[] currentActionResults = null;
    private int currentActionIndex = -1;

    public void dispatchResponse(ServerResponseResult response) {
        assert response != null;

        try {
            Object[] actionResults = null;
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
                    } catch (Exception ex) {
                        Log.error("Error dispatching gwt client action: ", ex);
                        throwInServerInvocation(ex);
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
                continueServerInvocation(actionResults, new ErrorAsyncCallback<ServerResponseResult>() {
                    @Override
                    public void success(ServerResponseResult response) {
                        dispatchResponse(response);
                    }
                });
            }
        } catch (Exception e) {
            handleDispatchException(e);
        }
    }

    protected void handleDispatchException(Exception e) {
        MessageBox.showMessageBox(true, "Error", e.getMessage(), null);
        Log.error("Error dispatching ServerResponseResult: ", e);
    }

    protected abstract void throwInServerInvocation(Exception ex);

    protected abstract void continueServerInvocation(Object[] actionResults, AsyncCallback<ServerResponseResult> callback) ;

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
        //todo:
//        SC.say("Report should be opened here.");
    }

    public void execute(GDialogAction action) {
    }
        @Override
    public Object execute(GChooseClassAction action) {
        return null;
    }

    @Override
    public void execute(GMessageAction action) {
    }

    @Override
    public int execute(GConfirmAction action) {
        return 0;
    }

    @Override
    public void execute(GLogMessageAction action) {
        if (action.failed) {
            Log.error(action.message);
        } else {
            Log.debug(action.message);
        }
    }

    @Override
    public void execute(GRunPrintReportAction action) {
    }

    @Override
    public void execute(GRunOpenInExcelAction action) {
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
}
