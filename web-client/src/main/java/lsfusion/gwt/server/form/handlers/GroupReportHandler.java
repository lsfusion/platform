package lsfusion.gwt.server.form.handlers;

import lsfusion.base.Pair;
import lsfusion.gwt.server.FileUtils;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormActionHandler;
import lsfusion.gwt.shared.actions.form.GroupReport;
import lsfusion.gwt.shared.actions.form.GroupReportResult;
import jasperapi.FormPrintType;
import jasperapi.ReportGenerationData;
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

        FormPrintType printType = action.toExcel ? FormPrintType.XLSX : FormPrintType.PDF;
        ReportGenerationData reportData = form.remoteForm.getReportData(action.requestIndex, defaultLastReceivedRequestIndex, action.groupObjectID, printType, converter.convertFormUserPreferences(action.preferences));

        Pair<String, String> report = FileUtils.exportReport(printType, reportData);
        return new GroupReportResult(report.first, report.second);
    }


}
