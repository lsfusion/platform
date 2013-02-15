package platform.gwt.form.client.form.dispatch;

import com.google.gwt.user.client.rpc.AsyncCallback;
import platform.gwt.form.client.form.ui.GFormController;
import platform.gwt.form.client.form.ui.classes.ClassChosenHandler;
import platform.gwt.base.client.ui.DialogBoxHelper;
import platform.gwt.form.client.form.ui.dialog.WindowHiddenHandler;
import platform.gwt.form.client.log.GLog;
import platform.gwt.form.shared.actions.form.ServerResponseResult;
import platform.gwt.form.shared.view.actions.*;
import platform.gwt.form.shared.view.classes.GObjectClass;

public class GFormActionDispatcher extends GwtActionDispatcher {
    protected final GFormController form;

    public GFormActionDispatcher(GFormController form) {
        this.form = form;
    }

    @Override
    protected void postDispatchResponse(ServerResponseResult response) {
        if (response.pendingRemoteChanges) {
            form.processRemoteChanges();
        }
    }

    @Override
    protected void continueServerInvocation(Object[] actionResults, AsyncCallback<ServerResponseResult> callback) {
        form.continueServerInvocation(actionResults, callback);
    }

    @Override
    protected void throwInServerInvocation(Exception ex) {
        form.throwInServerInvocation(ex);
    }

    @Override
    public void execute(GReportAction action) {
        form.openReportWindow(action.reportFileName);
    }

    @Override
    public void execute(final GFormAction action) {
        if (action.modalityType.isModal()) {
            pauseDispatching();
        }
        form.openForm(action.form, action.modalityType, new WindowHiddenHandler() {
            @Override
            public void onHidden() {
                if (action.modalityType.isModal()) {
                    continueDispatching();
                }
            }
        });
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
    public int execute(GConfirmAction action) {
        pauseDispatching();
        form.blockingConfirm(action.caption, action.message, new DialogBoxHelper.CloseCallback() {
            @Override
            public void closed(DialogBoxHelper.OptionType chosenOption) {
                continueDispatching(chosenOption.asInteger());
            }
        });

        return 0;
    }

    @Override
    public void execute(GLogMessageAction action) {
        if (GLog.isLogPanelVisible || action.failed) {
            super.execute(action);
        } else {
            pauseDispatching();
            form.blockingMessage(action.failed, "LS Fusion", action.message, new DialogBoxHelper.CloseCallback() {
                @Override
                public void closed(DialogBoxHelper.OptionType chosenOption) {
                    continueDispatching();
                }
            });
        }
    }

    @Override
    public void execute(GHideFormAction action) {
        form.hideForm();
    }

    @Override
    public void execute(GProcessFormChangesAction action) {
        form.applyRemoteChanges(action.formChanges);
    }
}
