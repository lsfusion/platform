package platform.gwt.form2.client.form.dispatch;

import com.google.gwt.user.client.rpc.AsyncCallback;
import platform.gwt.form2.client.events.OpenFormEvent;
import platform.gwt.form2.client.form.classes.ClassChosenHandler;
import platform.gwt.form2.client.form.ui.GFormController;
import platform.gwt.form2.client.form.ui.WindowHiddenHandler;
import platform.gwt.form2.client.form.ui.dialog.MessageBox;
import platform.gwt.form2.shared.actions.form.ServerResponseResult;
import platform.gwt.view2.actions.*;
import platform.gwt.view2.classes.GObjectClass;

public class GwtFormActionDispatcher extends GwtActionDispatcher {
    protected final GFormController form;

    public GwtFormActionDispatcher(GFormController form) {
        this.form = form;
    }

    @Override
    public void execute(GProcessFormChangesAction action) {
        form.applyRemoteChanges(action.formChanges);
    }

    @Override
    protected void continueServerInvocation(Object[] actionResults, AsyncCallback<ServerResponseResult> callback) {
        form.contiueServerInvocation(actionResults, callback);
    }

    @Override
    protected void throwInServerInvocation(Exception ex) {
        form.throwInServerInvocation(ex);
    }

    @Override
    public void execute(GReportAction action) {
        super.execute(action);
    }

    @Override
    public void execute(GFormAction action) {
        if (action.isModal) {
            pauseDispatching();
            form.showModalForm(action.form, new WindowHiddenHandler() {
                @Override
                public void onHidden() {
                    continueDispatching();
                }
            });
        } else {
            OpenFormEvent.fireEvent(action.form);
        }
    }

    @Override
    public void execute(GDialogAction action) {
        pauseDispatching();
        form.showModalDialog(action.dialog, new WindowHiddenHandler() {
            @Override
            public void onHidden() {
                continueDispatching();
            }
        });
    }

    @Override
    public Object execute(GChooseClassAction action) {
        pauseDispatching();
        form.showClassDialog(action.baseClass, action.defaultClass, action.concreate, new ClassChosenHandler() {
            @Override
            public void onClassChosen(GObjectClass chosenClass) {
                continueDispatching(chosenClass == null ? null : chosenClass.ID);
            }
        });
        return null;
    }

    @Override
    public void execute(GMessageAction action) {
        pauseDispatching();
        form.blockingMessage(action.caption, action.message, new MessageBox.CloseCallback() {
            @Override
            public void closed(boolean okPressed) {
                continueDispatching();
            }
        });
    }

    @Override
    public int execute(GConfirmAction action) {
        pauseDispatching();
        form.blockingConfirm(action.caption, action.message, new MessageBox.CloseCallback() {
            @Override
            public void closed(boolean okPressed) {
                continueDispatching(okPressed ? YES_OPTION : NO_OPTION);
            }
        });

        return 0;
    }

    @Override
    public void execute(GLogMessageAction action) {
        pauseDispatching();
        form.blockingMessage(action.failed, "", action.message, new MessageBox.CloseCallback() {
            @Override
            public void closed(boolean okPressed) {
                continueDispatching();
            }
        });
    }

    @Override
    public void execute(GRunPrintReportAction action) {
        form.runPrintReport(action.getReportSID());
    }

    @Override
    public void execute(GRunOpenInExcelAction action) {
        form.runOpenInExcel(action.getReportSID());
    }

    @Override
    public void execute(GHideFormAction action) {
        form.hideForm();
    }
}
