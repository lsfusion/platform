package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.CollapseGroupObjectRecursive;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.BatchRequestHandler;
import lsfusion.interop.form.remote.BatchRequest;

public class CollapseGroupObjectRecursiveHandler extends BatchRequestHandler<CollapseGroupObjectRecursive> {

    public CollapseGroupObjectRecursiveHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public BatchRequest getBatchRequest(CollapseGroupObjectRecursive action) {
        return new BatchRequest.CollapseGroupObjectRecursive(action.groupObjectId, action.current);
    }
}
