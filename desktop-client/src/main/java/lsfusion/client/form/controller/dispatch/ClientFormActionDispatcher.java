package lsfusion.client.form.controller.dispatch;

import com.google.common.base.Throwables;
import lsfusion.client.controller.MainController;
import lsfusion.client.controller.dispatch.DispatcherListener;
import lsfusion.client.controller.dispatch.SwingClientActionDispatcher;
import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.view.ClientFormDockable;
import lsfusion.interop.action.*;
import lsfusion.interop.base.remote.PendingRemoteInterface;
import lsfusion.interop.base.remote.RemoteRequestInterface;
import lsfusion.interop.form.ModalityType;

import java.awt.*;
import java.io.IOException;

public abstract class ClientFormActionDispatcher extends SwingClientActionDispatcher {

    public ClientFormActionDispatcher(DispatcherListener dispatcherListener) {
        super(dispatcherListener);
    }

    public abstract ClientFormController getFormController();

    @Override
    protected PendingRemoteInterface getRemote() {
        return getFormController().getRemoteForm();
    }

    @Override
    protected Container getDialogParentContainer() {
        return getFormController().getLayout();
    }

    @Override
    protected RmiQueue getRmiQueue() {
        return getFormController().getRmiQueue();
    }

    @Override
    protected RemoteRequestInterface getRemoteRequestInterface() {
        return getFormController().getRemoteRequestInterface();
    }

    @Override
    protected boolean canShowDockedModal() {
        return getFormController().canShowDockedModal();
    }

    @Override
    protected void beforeModalActionInSameEDT(boolean blockView) {
        getFormController().block(blockView);
    }

    @Override
    protected void setBlockingForm(ClientFormDockable blockingForm) {
        getFormController().setBlockingForm(blockingForm);
    }

    @Override
    protected void afterModalActionInSameEDT(boolean unblockView) {
        getFormController().unblock(unblockView);
    }

    public void execute(RunEditReportClientAction action) {
        getFormController().runEditReport(action.customReportPathList);
    }

    public void execute(HideFormClientAction action) {
        getFormController().hideForm();
    }

    public void execute(ProcessFormChangesClientAction action) {
        try {
            getFormController().applyFormChanges(action.requestIndex, action.formChanges, false);
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }

    public void execute(EditNotPerformedClientAction action) {
    }

    public void execute(UpdateEditValueClientAction action) {
    }

    @Override
    public void execute(AsyncGetRemoteChangesClientAction action) {
        getFormController().getRemoteChanges(true, action.forceLocalEvents);
    }

    @Override
    public void execute(ChangeColorThemeClientAction action) {
        MainController.changeColorTheme(action.colorTheme);
    }
}
