package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.CollapseGroupObject;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.BatchRequestHandler;
import lsfusion.interop.form.remote.BatchRequest;

public class CollapseGroupObjectHandler extends BatchRequestHandler<CollapseGroupObject> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public CollapseGroupObjectHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public BatchRequest getBatchRequest(CollapseGroupObject action) {
        byte[] keyValues = gwtConverter.convertOrCast(action.value);
        return new BatchRequest.CollapseGroupObject(action.groupObjectId, keyValues);
    }
}
