package lsfusion.gwt.form.server.form.handlers;

import lsfusion.gwt.form.server.FileUtils;
import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.server.form.spring.FormSessionObject;
import lsfusion.gwt.form.server.convert.GwtToClientConverter;
import lsfusion.gwt.form.server.form.FormActionHandler;
import lsfusion.gwt.form.shared.actions.form.GroupReport;
import lsfusion.interop.FormPrintType;
import lsfusion.interop.form.ReportGenerationData;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.general.StringResult;

import java.io.IOException;

public class GroupReportHandler extends FormActionHandler<GroupReport, StringResult> {
    public GroupReportHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public StringResult executeEx(GroupReport action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);

        GwtToClientConverter converter = GwtToClientConverter.getInstance();

        FormPrintType printType = action.toExcel ? FormPrintType.XLSX : FormPrintType.PDF;
        ReportGenerationData reportData = form.remoteForm.getReportData(action.requestIndex, defaultLastReceivedRequestIndex, action.groupObjectID, printType, converter.convertFormUserPreferences(action.preferences));

        return new StringResult(FileUtils.exportReport(printType, reportData));
    }


}
