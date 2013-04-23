package platform.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.client.form.TableTransferHandler;
import platform.gwt.form.server.FormDispatchServlet;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.shared.actions.form.PasteExternalTable;
import platform.gwt.form.shared.actions.form.ServerResponseResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PasteExternalTableHandler extends ServerResponseActionHandler<PasteExternalTable> {
    public PasteExternalTableHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(PasteExternalTable action, ExecutionContext context) throws DispatchException, IOException {
        List<List<String>> stringTable = TableTransferHandler.getClipboardTable(action.line);

        int tableColumns = 0;
        if (!stringTable.isEmpty()) {
            tableColumns = stringTable.get(0).size();
        }
        int columnsToInsert = Math.min(tableColumns, action.properties.size());

        List<Integer> propertyIDs = new ArrayList<Integer>();
        for (int i = 0; i < columnsToInsert; i++) {
            propertyIDs.add(action.properties.get(i).ID);
        }

        List<List<String>> dataTable = new ArrayList<List<String>>();
        for (List<String> row : stringTable) {
            List<String> pasteTableRow = new ArrayList<String>();
            int itemIndex = -1;
            for (String item : row) {
                itemIndex++;
                if (itemIndex <= columnsToInsert - 1) {
                    pasteTableRow.add(item);
                }
            }
            dataTable.add(pasteTableRow);
        }

        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(form, form.remoteForm.pasteExternalTable(action.requestIndex, propertyIDs, dataTable));
    }
}
