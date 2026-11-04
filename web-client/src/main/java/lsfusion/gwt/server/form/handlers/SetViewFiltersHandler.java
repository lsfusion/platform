package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.SetViewFilters;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.BatchRequestHandler;
import lsfusion.interop.form.remote.BatchRequest;

public class SetViewFiltersHandler extends BatchRequestHandler<SetViewFilters> {

    public SetViewFiltersHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public BatchRequest getBatchRequest(SetViewFilters action) {
        return new BatchRequest.SetViewFilters(SetUserFiltersHandler.serializeFilters(action.filters), action.pageSize);
    }
}
