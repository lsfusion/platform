package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.SelectAll;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.BatchRequestHandler;
import lsfusion.interop.form.remote.BatchRequest;

public class SelectAllHandler extends BatchRequestHandler<SelectAll> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public SelectAllHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public BatchRequest getBatchRequest(SelectAll action) {
        int size = action.changeSelectionProps.length;
        byte[][] changeSelectionColumnKeys = new byte[size][];
        for (int i = 0; i < size; i++) {
            changeSelectionColumnKeys[i] = gwtConverter.convertOrCast(action.changeSelectionColumnKeys[i]);
        }

        return new BatchRequest.SelectAll(action.groupId, action.changeSelectionProps, changeSelectionColumnKeys, action.changeSelectionValues);
    }
}
