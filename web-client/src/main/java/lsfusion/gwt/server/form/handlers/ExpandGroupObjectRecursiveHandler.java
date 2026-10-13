package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ExpandGroupObjectRecursive;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.BatchRequestHandler;
import lsfusion.interop.form.remote.BatchRequest;

public class ExpandGroupObjectRecursiveHandler extends BatchRequestHandler<ExpandGroupObjectRecursive> {

    public ExpandGroupObjectRecursiveHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public BatchRequest getBatchRequest(ExpandGroupObjectRecursive action) {
        return new BatchRequest.ExpandGroupObjectRecursive(action.groupObjectId, action.current);
    }
}
