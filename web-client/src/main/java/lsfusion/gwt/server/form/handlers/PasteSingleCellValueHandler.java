package lsfusion.gwt.server.form.handlers;

import com.google.common.base.Throwables;
import lsfusion.gwt.client.controller.remote.action.form.PasteSingleCellValue;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.remote.RemoteFormInterface;
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
    public ServerResponseResult executeEx(final PasteSingleCellValue action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, new RemoteCall() {
            public ServerResponse call(RemoteFormInterface remoteForm) throws RemoteException {
                byte[] fullKey = gwtConverter.convertOrCast(action.fullKey);

                byte[] value;
                try {
                    value = serializeObject(
                            gwtConverter.convertOrCast(action.value)
                    );
                } catch (IOException e) {
                    throw Throwables.propagate(e);
                }

                return remoteForm.pasteMulticellValue(action.requestIndex, action.lastReceivedRequestIndex, singletonMap(action.propertyId, singletonList(fullKey)), singletonMap(action.propertyId, value));
            }
        });
    }
}
