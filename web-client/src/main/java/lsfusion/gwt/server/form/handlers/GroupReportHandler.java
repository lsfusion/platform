package lsfusion.gwt.server.form.handlers;

import lsfusion.base.Pair;
import lsfusion.gwt.client.controller.remote.action.form.GroupReport;
import lsfusion.gwt.client.controller.remote.action.form.GroupReportResult;
import lsfusion.gwt.server.FileUtils;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormActionHandler;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.interop.form.print.FormPrintType;
import lsfusion.interop.form.print.ReportGenerationData;
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

        FormPrintType printType = FormPrintType.XLSX;
        ReportGenerationData reportData = form.remoteForm.getReportData(action.requestIndex, action.lastReceivedRequestIndex, action.groupObjectID, printType, converter.convertFormUserPreferences(action.preferences));

        Pair<String, String> report = FileUtils.exportReport(printType, reportData, servlet.getNavigatorProvider().getRemoteLogics());
        return new GroupReportResult(report.first, report.second);
    }


}
