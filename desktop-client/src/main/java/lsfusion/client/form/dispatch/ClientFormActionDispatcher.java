package lsfusion.client.form.dispatch;

import com.google.common.base.Throwables;
import jasperapi.ReportGenerator;
import lsfusion.client.Main;
import lsfusion.client.SwingUtils;
import lsfusion.client.dock.ClientFormDockable;
import lsfusion.client.form.ClientFormController;
import lsfusion.client.form.DispatcherListener;
import lsfusion.client.report.ClientReportUtils;
import lsfusion.interop.FormPrintType;
import lsfusion.interop.ModalityType;
import lsfusion.interop.action.*;
import lsfusion.interop.form.ServerResponse;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.rmi.RemoteException;

import static lsfusion.client.ClientResourceBundle.getString;

public abstract class ClientFormActionDispatcher extends SwingClientActionDispatcher {

    public ClientFormActionDispatcher(DispatcherListener dispatcherListener) {
        super(dispatcherListener);
    }

    public abstract ClientFormController getFormController();

    @Override
    protected Container getDialogParentContainer() {
        return getFormController().getLayout();
    }

    @Override
    protected ServerResponse throwInServerInvocation(long requestIndex, int continueIndex, Throwable t) throws IOException {
        return getFormController().throwInServerInvocation(requestIndex, continueIndex, t);
    }

    @Override
    public ServerResponse continueServerInvocation(long requestIndex, int continueIndex, Object[] actionResults) throws RemoteException {
        return getFormController().continueServerInvocation(requestIndex, continueIndex, actionResults);
    }

    @Override
    public void execute(FormClientAction action) {
        if (action.modalityType == ModalityType.DOCKED_MODAL && !getFormController().canShowDockedModal()) {
            action.modalityType = ModalityType.MODAL;
        }
        super.execute(action);
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

    @Override
    public Integer execute(ReportClientAction action) {
        Integer pageCount = null;
        try {
            if (action.printType == FormPrintType.AUTO) {
                ClientReportUtils.autoprintReport(action.generationData, action.printerName);
            } else if (action.printType != FormPrintType.PRINT) {
                int editChoice = JOptionPane.NO_OPTION;
                if (action.inDevMode && action.reportPathList.isEmpty()) {
                    editChoice = SwingUtils.showConfirmDialog(Main.frame, 
                            getString("layout.menu.file.create.custom.report.choice"),
                            getString("layout.menu.file.create.custom.report.title"),
                            JOptionPane.QUESTION_MESSAGE,
                            false);
                    if (editChoice == JOptionPane.YES_OPTION) {
                        Main.addReportPathList(action.reportPathList, action.formSID);
                    }
                }
                if (editChoice == JOptionPane.NO_OPTION) {
                    ReportGenerator.exportAndOpen(action.generationData, action.printType, action.sheetName, action.password);
                }
            } else {
                if (action.inDevMode) {
                    pageCount = Main.frame.runReport(getFormController(), action.reportPathList, action.formSID, action.isModal, action.generationData, action.printerName);
                } else {
                    pageCount = Main.frame.runReport(action.isModal, action.generationData, action.printerName, null);
                }
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        }
        return pageCount;
    }

    public void execute(HideFormClientAction action) {
        getFormController().hideForm();
    }

    public void execute(ProcessFormChangesClientAction action) {
        try {
            getFormController().applyFormChanges(action.requestIndex, action.formChanges);
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
