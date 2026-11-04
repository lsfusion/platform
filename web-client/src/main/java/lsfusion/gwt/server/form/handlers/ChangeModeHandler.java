package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ChangeMode;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.BatchRequestHandler;
import lsfusion.interop.form.property.PropertyGroupType;
import lsfusion.interop.form.remote.BatchRequest;

public class ChangeModeHandler extends BatchRequestHandler<ChangeMode> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ChangeModeHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public BatchRequest getBatchRequest(ChangeMode action) {
        byte[][] columnKeys = null;
        PropertyGroupType aggrType = null;

        if(action.propertyIDs != null) {
            int size = action.propertyIDs.length;
            columnKeys = new byte[size][];
            for (int i = 0; i < size; i++)
                columnKeys[i] = gwtConverter.convertOrCast(action.columnKeys[i]);
            aggrType = gwtConverter.convertOrCast(action.aggrType);
        }

        return new BatchRequest.ChangeMode(action.groupObjectID, action.setGroup, action.propertyIDs, columnKeys, action.aggrProps, aggrType, action.pageSize, action.forceRefresh, gwtConverter.convertOrCast(action.updateMode), gwtConverter.convertOrCast(action.viewType));
    }
}
