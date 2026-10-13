package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.SetPropertyOrders;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.BatchRequestHandler;
import lsfusion.interop.form.remote.BatchRequest;

import java.util.ArrayList;
import java.util.List;

public class SetPropertyOrdersHandler extends BatchRequestHandler<SetPropertyOrders> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public SetPropertyOrdersHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public BatchRequest getBatchRequest(SetPropertyOrders action) {
        List<byte[]> columnKeys = new ArrayList<>();
        for(GGroupObjectValue columnKey : action.columnKeyList) {
            columnKeys.add(gwtConverter.convertOrCast(columnKey));
        }
        return new BatchRequest.SetPropertyOrders(action.groupObjectID, action.propertyList, columnKeys, action.orderList);
    }
}
