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
    protected void postDispatchResponse(ServerResponse serverResponse) throws IOException {
        assert !serverResponse.resumeInvocation;
        if(serverResponse.pendingRemoteChanges)
            getFormController().processRemoteChanges(true);
    }

    @Override
    protected Container getDialogParentContainer() {
        return getFormController().getComponent();
    }

    @Override
    protected void throwInServerInvocation(Exception ex) throws IOException {
        getFormController().throwInServerInvocation(ex);
    }

    @Override
    public ServerResponse continueServerInvocation(Object[] actionResults) throws RemoteException {
        return getFormController().continueServerInvocation(actionResults);
    }

    public void execute(RunPrintReportClientAction action) {
        getFormController().runPrintReport();
    }

    public void execute(RunOpenInExcelClientAction action) {
        getFormController().runOpenInExcel();
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
