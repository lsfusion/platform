package lsfusion.client.form.dispatch;

import com.google.common.base.Throwables;
import jasperapi.ReportGenerator;
import lsfusion.client.ClientReportUtils;
import lsfusion.client.Main;
import lsfusion.client.form.ClientFormController;
import lsfusion.interop.FormPrintType;
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
        return getFormController().getLayout();
    }

    @Override
    protected void throwInServerInvocation(Throwable t) throws IOException {
        getFormController().throwInServerInvocation(t);
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
        getFormController().runPrintReport(action.isDebug);
    }

    public void execute(RunOpenInExcelClientAction action) {
        getFormController().runOpenInExcel();
    }

    public void execute(RunEditReportClientAction action) {
        getFormController().runEditReport();
    }

    @Override
    public void execute(ReportClientAction action) {
        try {
            if (action.printType == FormPrintType.AUTO) {
                ClientReportUtils.autoprintReport(action.generationData);
            } else if (action.printType == FormPrintType.XLS) {
                ReportGenerator.exportToExcelAndOpen(action.generationData, Main.timeZone);    
            } else if (action.printType == FormPrintType.PDF) {
                ReportGenerator.exportToPdfAndOpen(action.generationData, Main.timeZone);
            } else {
                if (action.isDebug) {
                    Main.frame.runReport(action.reportSID, action.isModal, action.generationData);
                } else {
                    Main.frame.runReport(action.reportSID, action.isModal, action.generationData, null);
                }
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        }
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

    public void execute(EditNotPerformedClientAction action) {
    }

    public void execute(UpdateEditValueClientAction action) {
    }

    @Override
    public void execute(AsyncGetRemoteChangesClientAction action) {
        getFormController().getRemoteChanges(true);
    }
}
