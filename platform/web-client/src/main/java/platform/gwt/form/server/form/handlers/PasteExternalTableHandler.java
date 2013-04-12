package platform.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.client.form.TableTransferHandler;
import platform.client.logics.classes.*;
import platform.gwt.form.server.FormDispatchServlet;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.shared.actions.form.PasteExternalTable;
import platform.gwt.form.shared.actions.form.ServerResponseResult;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.classes.*;

import java.io.IOException;
import java.text.ParseException;
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

        List<List<Object>> dataTable = new ArrayList<List<Object>>();
        for (List<String> row : stringTable) {
            List<Object> pasteTableRow = new ArrayList<Object>();
            int itemIndex = -1;
            for (String item : row) {
                itemIndex++;
                if (itemIndex <= columnsToInsert - 1) {
                    GPropertyDraw property = action.properties.get(itemIndex);
                    pasteTableRow.add(parseString(property, item));
                }
            }
            dataTable.add(pasteTableRow);
        }

        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(form, form.remoteForm.pasteExternalTable(action.requestIndex, propertyIDs, dataTable));
    }

    private Object parseString(GPropertyDraw property, String s) {
        if (s == null) {
            return null;
        }
        try {
            if (property.baseType instanceof GDateType) {
                return ClientDateClass.instance.parseString(s);
            } else if (property.baseType instanceof GDateTimeType) {
                return ClientDateTimeClass.instance.parseString(s);
            } else if (property.baseType instanceof GTimeType) {
                return ClientTimeClass.instance.parseString(s);
            } else if (property.baseType instanceof GDoubleType) {
                return ClientDoubleClass.instance.parseString(s);
            } else if (property.baseType instanceof GColorType) {
                return ClientColorClass.instance.parseString(s);
            } else {
                return property.parseString(s);
            }
        } catch (ParseException e) {
            return null;
        }
    }
}
