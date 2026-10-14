package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ChangeGroupObject;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.BatchRequestHandler;
import lsfusion.interop.form.remote.BatchRequest;

public class ChangeGroupObjectHandler extends BatchRequestHandler<ChangeGroupObject> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ChangeGroupObjectHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public BatchRequest getBatchRequest(ChangeGroupObject action) {
        byte[] keyValues = gwtConverter.convertOrCast(action.keyValues);
        return new BatchRequest.ChangeGroupObject(action.groupId, keyValues, gwtConverter.convertOrCast(action.changeSelection));
    }
}
