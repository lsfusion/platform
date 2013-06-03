package lsfusion.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.general.StringResult;
import lsfusion.gwt.form.server.FileUtils;
import lsfusion.gwt.form.server.FormDispatchServlet;
import lsfusion.gwt.form.server.FormSessionObject;
import lsfusion.gwt.form.shared.actions.form.SingleGroupReport;
import lsfusion.interop.form.ReportGenerationData;

import java.io.IOException;

public class SingleGroupReportHandler extends FormActionHandler<SingleGroupReport, StringResult> {
    public SingleGroupReportHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public StringResult executeEx(SingleGroupReport action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        ReportGenerationData reportData = form.remoteForm.getReportData(action.requestIndex, action.groupObjectID, action.toExcel, null);

        return new StringResult(FileUtils.exportReport(action.toExcel, reportData));
    }


}
