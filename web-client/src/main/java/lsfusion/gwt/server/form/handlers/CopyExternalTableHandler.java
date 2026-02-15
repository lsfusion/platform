package lsfusion.gwt.server.form.handlers;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.gwt.client.controller.remote.action.form.CopyExternalTable;
import lsfusion.gwt.client.controller.remote.action.form.CopyExternalTableResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormActionHandler;
import lsfusion.http.provider.form.FormSessionObject;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import static lsfusion.base.BaseUtils.deserializeObject;
import static lsfusion.base.BaseUtils.serializeObject;

public class CopyExternalTableHandler extends FormActionHandler<CopyExternalTable, CopyExternalTableResult> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public CopyExternalTableHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public CopyExternalTableResult executeEx(CopyExternalTable action, ExecutionContext context) throws RemoteException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);

        // Convert columnKeys (same as in PasteExternalTableHandler)
        List<byte[]> columnKeys = new ArrayList<>();
        for (int i = 0; i < action.columnKeys.size(); i++) {
            columnKeys.add(gwtConverter.convertOrCast(action.columnKeys.get(i)));
        }

        // Call server
        Pair<List<List<byte[]>>, ArrayList<ArrayList<String>>> result = form.remoteForm.copyExternalTable(
                action.requestIndex, action.lastReceivedRequestIndex, action.propertyIdList, columnKeys);

        // Convert to ArrayList without extra copies
        ArrayList<ArrayList<Object>> values = new ArrayList<>();
        for (List<byte[]> row : result.first) {
            ArrayList<Object> valueRow = new ArrayList<>();
            for(byte[] rowValue : row) {
                try {
                    valueRow.add(gwtConverter.convertOrCast(deserializeObject(rowValue)));
                } catch (IOException e) {
                    throw Throwables.propagate(e);
                }
            }
            values.add(valueRow);
        }

        return new CopyExternalTableResult(values, result.second);
    }
}
