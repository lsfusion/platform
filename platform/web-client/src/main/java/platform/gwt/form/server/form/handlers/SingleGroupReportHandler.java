package platform.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.general.StringResult;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.server.RemoteServiceImpl;
import platform.gwt.form.server.ReportExporter;
import platform.gwt.form.shared.actions.form.SingleGroupReport;
import platform.interop.form.ReportGenerationData;

import java.io.IOException;

public class SingleGroupReportHandler extends FormActionHandler<SingleGroupReport, StringResult> {
    public SingleGroupReportHandler(RemoteServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public StringResult executeEx(SingleGroupReport action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        ReportGenerationData reportData = form.remoteForm.getReportData(action.requestIndex, action.groupObjectID, action.toExcel, null);

        return new StringResult(ReportExporter.exportReport(getSession(), action.toExcel, reportData));
    }


}
