package platform.client.form.dispatch;

import com.google.common.base.Throwables;
import platform.client.form.ClientFormController;
import platform.interop.action.*;
import platform.interop.form.FormUserPreferences;
import platform.interop.form.ServerResponse;

import java.awt.*;
import java.io.IOException;
import java.rmi.RemoteException;

public abstract class ClientFormActionDispatcher extends SwingClientActionDispatcher {

    public abstract ClientFormController getFormController();

    @Override
    protected Container getDialogParentContainer() {
        return getFormController().getComponent();
    }

    protected FormUserPreferences getFormUserPreferences() {
        return getFormController().getUserPreferences();
    }

    @Override
    protected void preDispatchResponse(ServerResponse serverResponse) {
        setFormBusy(true);
    }

    @Override
    protected void postDispatchResponse(ServerResponse serverResponse) throws IOException {
        setFormBusy(false);
    }

    @Override
    protected void handleDispatchException(Exception e) throws IOException {
        setFormBusy(false);
        super.handleDispatchException(e);
    }

    @Override
    public ServerResponse continueServerInvocation(Object[] actionResults) throws RemoteException {
        return getFormController().continueServerInvocation(actionResults);
    }

    private void setFormBusy(boolean busy) {
        getFormController().setBusy(busy);
    }

    public void execute(PrintPreviewClientAction action) {
        getFormController().runReport();
    }

    public void execute(RunExcelClientAction action) {
        getFormController().runExcel();
    }

    public void execute(RunEditReportClientAction action) {
        getFormController().runEditReport();
    }

    public void execute(HideFormClientAction action) {
        getFormController().hideForm();
    }

    public void execute(ProcessFormChangesClientAction action) {
        try {
            getFormController().applyFormChanges(action.formChanges);
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }

    public void execute(UpdateCurrentClassClientAction action) {
        getFormController().updateCurrentClass(action.currentClassId);
    }
}
