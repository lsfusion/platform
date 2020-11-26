package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.SetPropertyOrders;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.remote.RemoteFormInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class SetPropertyOrdersHandler extends FormServerResponseActionHandler<SetPropertyOrders> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public SetPropertyOrdersHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(final SetPropertyOrders action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, new RemoteCall() {
            public ServerResponse call(RemoteFormInterface remoteForm) throws RemoteException {
                List<byte[]> columnKeys = new ArrayList<>();
                for(GGroupObjectValue columnKey : action.columnKeyList) {
                    columnKeys.add(gwtConverter.convertOrCast(columnKey));
                }

                return remoteForm.setPropertyOrders(action.requestIndex, action.lastReceivedRequestIndex, action.groupObjectID, action.propertyList, columnKeys, action.orderList);
            }
        });
    }
}
