package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.SetRegularFilter;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.BatchRequestHandler;
import lsfusion.interop.form.remote.BatchRequest;

public class SetRegularFilterHandler extends BatchRequestHandler<SetRegularFilter> {
    public SetRegularFilterHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public BatchRequest getBatchRequest(SetRegularFilter action) {
        return new BatchRequest.SetRegularFilter(action.groupId, action.filterId);
    }
}
