package lsfusion.gwt.form.client.form.dispatch;

import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.base.client.ui.DialogBoxHelper;
import lsfusion.gwt.form.client.form.ui.GFormController;
import lsfusion.gwt.form.client.form.ui.classes.ClassChosenHandler;
import lsfusion.gwt.form.client.form.ui.dialog.WindowHiddenHandler;
import lsfusion.gwt.form.client.log.GLog;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.form.shared.view.actions.*;
import lsfusion.gwt.form.shared.view.classes.GObjectClass;
import lsfusion.gwt.form.shared.view.grid.EditEvent;
import lsfusion.gwt.form.shared.view.window.GModalityType;

public class GFormActionDispatcher extends GwtActionDispatcher {
    protected final GFormController form;

    private EditEvent latestEditEvent;

    public GFormActionDispatcher(GFormController form) {
        this.form = form;
    }

    public void setLatestEditEvent(EditEvent latestEditEvent) {
        this.latestEditEvent = latestEditEvent;
    }

    @Override
    protected void continueServerInvocation(Object[] actionResults, AsyncCallback<ServerResponseResult> callback) {
        form.continueServerInvocation(actionResults, callback);
    }

    @Override
    protected void throwInServerInvocation(Throwable t, AsyncCallback<ServerResponseResult> callback) {
        form.throwInServerInvocation(t, callback);
    }

    @Override
    public void execute(GReportAction action) {
        form.openReportWindow(action.reportFileName);
    }

    @Override
    public void execute(final GFormAction action) {
        if (form.isModal() && action.modalityType == GModalityType.DOCKED_MODAL) {
            action.modalityType = GModalityType.MODAL;
        }

        if (action.modalityType.isModal()) {
            pauseDispatching();
        }
        form.openForm(action.form, action.modalityType, latestEditEvent, new WindowHiddenHandler() {
            @Override
            public void onHidden() {
                if (action.modalityType.isModal()) {
                    continueDispatching();
                }
            }
        });
    }

    @Override
    public void execute(GRunOpenReportAction action) {
        form.runGroupReport(null, action.openInExcel);
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
        form.blockingConfirm(action.caption, action.message, action.cancel, action.timeout, action.initialValue, new DialogBoxHelper.CloseCallback() {
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
            form.blockingMessage(action.failed, "lsFusion", action.message, new DialogBoxHelper.CloseCallback() {
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

    @Override
    public void execute(GAsyncGetRemoteChangesAction action) {
        form.getRemoteChanges();
    }

    @Override
    public void execute(GFocusAction action) {
        form.focusProperty(action.propertyId);
    }

    @Override
    public void execute(GActivateTabAction action) {
        form.activateTab(action.formSID, action.tabSID);
    }
}
