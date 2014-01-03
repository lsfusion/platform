package lsfusion.gwt.form.server.form.handlers;

import lsfusion.gwt.form.server.FormDispatchServlet;
import lsfusion.gwt.form.server.FormSessionObject;
import lsfusion.gwt.form.server.convert.GwtToClientConverter;
import lsfusion.gwt.form.shared.actions.form.PasteExternalTable;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static lsfusion.base.BaseUtils.serializeObject;

public class PasteExternalTableHandler extends ServerResponseActionHandler<PasteExternalTable> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public PasteExternalTableHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(PasteExternalTable action, ExecutionContext context) throws DispatchException, IOException {
        List<List<byte[]>> values = new ArrayList<List<byte[]>>();
        for (List<Object> gRowValues : action.values) {
            List<byte[]> rowValues = new ArrayList<byte[]>();

            for (Object gRowValue : gRowValues) {
                Object oCell = gwtConverter.convertOrCast(gRowValue, servlet.getBLProvider());
                rowValues.add(serializeObject(oCell));
            }

            values.add(rowValues);
        }

        List<byte[]> columnKeys = new ArrayList<byte[]>();
        for (int i = 0; i < action.columnKeys.size(); i++) {
            columnKeys.add((byte[]) gwtConverter.convertOrCast(action.columnKeys.get(i)));
        }

        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(form, form.remoteForm.pasteExternalTable(action.requestIndex, action.propertyIdList, columnKeys, values));
    }
}
