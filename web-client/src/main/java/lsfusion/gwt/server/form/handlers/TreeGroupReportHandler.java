package lsfusion.gwt.server.form.handlers;

import lsfusion.base.file.FileData;
import lsfusion.gwt.client.controller.remote.action.form.GroupReportResult;
import lsfusion.gwt.client.controller.remote.action.form.TreeGroupReport;
import lsfusion.gwt.server.FileUtils;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormActionHandler;
import lsfusion.http.provider.form.FormSessionObject;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class TreeGroupReportHandler extends FormActionHandler<TreeGroupReport, GroupReportResult> {
    public TreeGroupReportHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GroupReportResult executeEx(TreeGroupReport action, ExecutionContext context) throws RemoteException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);

        GwtToClientConverter converter = GwtToClientConverter.getInstance();

        Object reportData = form.remoteForm.getTreeGroupReportData(action.requestIndex, action.lastReceivedRequestIndex, action.groupObjectID, converter.convertFormUserPreferences(action.preferences));

        return new GroupReportResult(FileUtils.exportFile((FileData) reportData, null));
    }
}
