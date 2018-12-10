package lsfusion.gwt.client.form.form.dispatch;

import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.client.base.ui.DialogBoxHelper;
import lsfusion.gwt.client.form.form.ui.GFormController;
import lsfusion.gwt.client.form.form.ui.classes.ClassChosenHandler;
import lsfusion.gwt.client.form.form.ui.dialog.WindowHiddenHandler;
import lsfusion.gwt.client.form.log.GLog;
import lsfusion.gwt.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.shared.view.actions.*;
import lsfusion.gwt.shared.view.classes.GObjectClass;
import lsfusion.gwt.client.form.grid.EditEvent;
import lsfusion.gwt.shared.view.window.GModalityType;

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
        form.openForm(action.form, action.modalityType, action.forbidDuplicate, latestEditEvent, new WindowHiddenHandler() {
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

    //todo: по идее, action должен заливать куда-то в сеть выбранный локально файл
    @Override
    public String execute(GLoadLinkAction action) {
        return null;
    }

}
