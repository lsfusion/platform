package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.server.LSFusionDispatchServlet;
import lsfusion.gwt.server.form.provider.FormSessionObject;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.gwt.shared.actions.form.PasteExternalTable;
import lsfusion.gwt.shared.actions.form.ServerResponseResult;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static lsfusion.base.BaseUtils.serializeObject;

public class PasteExternalTableHandler extends FormServerResponseActionHandler<PasteExternalTable> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public PasteExternalTableHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(PasteExternalTable action, ExecutionContext context) throws DispatchException, IOException {
        List<List<byte[]>> values = new ArrayList<>();
        for (List<Object> gRowValues : action.values) {
            List<byte[]> rowValues = new ArrayList<>();

            for (Object gRowValue : gRowValues) {
                Object oCell = gwtConverter.convertOrCast(gRowValue);
                rowValues.add(serializeObject(oCell));
            }

            values.add(rowValues);
        }

        List<byte[]> columnKeys = new ArrayList<>();
        for (int i = 0; i < action.columnKeys.size(); i++) {
            columnKeys.add((byte[]) gwtConverter.convertOrCast(action.columnKeys.get(i)));
        }

        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(form, form.remoteForm.pasteExternalTable(action.requestIndex, defaultLastReceivedRequestIndex, action.propertyIdList, columnKeys, values));
    }
}
