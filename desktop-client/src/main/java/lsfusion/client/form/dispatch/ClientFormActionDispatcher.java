package lsfusion.client.form.dispatch;

import com.google.common.base.Throwables;
import lsfusion.client.form.ClientFormController;
import lsfusion.interop.ModalityType;
import lsfusion.interop.action.*;
import lsfusion.interop.form.ServerResponse;

import java.awt.*;
import java.io.IOException;
import java.rmi.RemoteException;

public abstract class ClientFormActionDispatcher extends SwingClientActionDispatcher {

    public abstract ClientFormController getFormController();

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

    @Override
    public void execute(FormClientAction action) {
        if (action.modalityType == ModalityType.DOCKED_MODAL && !getFormController().canShowDockedModal()) {
            action.modalityType = ModalityType.MODAL;
        }
        super.execute(action);
    }

    @Override
    protected void beforeShowDockedModalForm() {
        getFormController().block();
    }

    @Override
    protected void afterHideDockedModalForm() {
        getFormController().unblock();
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

    public void execute(EditNotPerformedClientAction action) {
    }

    public void execute(UpdateEditValueClientAction action) {
    }

    @Override
    public void execute(AsyncGetRemoteChangesClientAction action) {
        try {
            getFormController().getRemoteChanges(true);
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }
}
