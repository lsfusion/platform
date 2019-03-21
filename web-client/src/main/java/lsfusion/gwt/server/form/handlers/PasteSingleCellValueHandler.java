package lsfusion.gwt.server.form.handlers;

import com.google.common.base.Throwables;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.gwt.client.controller.remote.action.form.PasteSingleCellValue;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.io.IOException;
import java.rmi.RemoteException;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static lsfusion.base.BaseUtils.serializeObject;

public class PasteSingleCellValueHandler extends FormServerResponseActionHandler<PasteSingleCellValue> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public PasteSingleCellValueHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(PasteSingleCellValue action, ExecutionContext context) throws RemoteException {
        byte[] fullKey = gwtConverter.convertOrCast(action.fullKey);

        byte[] value;
        try {
            value = serializeObject(
                    gwtConverter.convertOrCast(action.value)
            );
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        FormSessionObject form = getFormSessionObject(action.formSessionID);

        return getServerResponseResult(
                form,
                form.remoteForm.pasteMulticellValue(action.requestIndex, defaultLastReceivedRequestIndex, singletonMap(action.propertyId, singletonList(fullKey)), singletonMap(action.propertyId, value))
        );
    }
}
