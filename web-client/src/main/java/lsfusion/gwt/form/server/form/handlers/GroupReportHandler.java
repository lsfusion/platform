package lsfusion.gwt.form.server.form.handlers;

import lsfusion.gwt.form.server.FileUtils;
import lsfusion.gwt.form.server.FormDispatchServlet;
import lsfusion.gwt.form.server.FormSessionObject;
import lsfusion.gwt.form.server.convert.GwtToClientConverter;
import lsfusion.gwt.form.shared.actions.form.GroupReport;
import lsfusion.interop.form.ReportGenerationData;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.general.StringResult;

import java.io.IOException;

public class GroupReportHandler extends FormActionHandler<GroupReport, StringResult> {
    public GroupReportHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public StringResult executeEx(GroupReport action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);

        GwtToClientConverter converter = GwtToClientConverter.getInstance();
        
        ReportGenerationData reportData = form.remoteForm.getReportData(action.requestIndex, -1, action.groupObjectID, action.toExcel, converter.convertFormUserPreferences(action.preferences));

        return new StringResult(FileUtils.exportReport(action.toExcel, reportData));
    }


}
