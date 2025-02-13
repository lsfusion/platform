package lsfusion.gwt.server.form.handlers;

import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.gwt.client.controller.remote.action.form.GroupReport;
import lsfusion.gwt.client.controller.remote.action.form.GroupReportResult;
import lsfusion.gwt.server.FileUtils;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.ClientActionToGwtConverter;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormActionHandler;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.interop.form.print.FormPrintType;
import lsfusion.interop.form.print.ReportGenerationData;
import lsfusion.interop.form.print.ReportGenerator;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class GroupReportHandler extends FormActionHandler<GroupReport, GroupReportResult> {
    public GroupReportHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GroupReportResult executeEx(GroupReport action, ExecutionContext context) throws RemoteException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);

        GwtToClientConverter converter = GwtToClientConverter.getInstance();

        Object reportData = form.remoteForm.getGroupReportData(action.requestIndex, action.lastReceivedRequestIndex, action.groupObjectID, converter.convertFormUserPreferences(action.preferences));
        if(!(reportData instanceof FileData))
            reportData = ClientActionToGwtConverter.exportToFileByteArray(servlet, (ReportGenerationData) reportData, FormPrintType.XLSX, null, null, action.jasperReportsIgnorePageMargins);

        return new GroupReportResult(FileUtils.exportFile((FileData) reportData));
    }


}
