package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ChangePropertyActive;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.BatchRequestHandler;
import lsfusion.interop.form.remote.BatchRequest;

public class ChangePropertyActiveHandler extends BatchRequestHandler<ChangePropertyActive> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ChangePropertyActiveHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public BatchRequest getBatchRequest(ChangePropertyActive action) {
        byte[][] changeSelectionColumnKeys = null;

        if(action.changeSelectionProps != null) {
            int size = action.changeSelectionProps.length;
            changeSelectionColumnKeys = new byte[size][];
            for (int i = 0; i < size; i++) {
                changeSelectionColumnKeys[i] = gwtConverter.convertOrCast(action.changeSelectionColumnKeys[i]);
            }
        }

        return new BatchRequest.ChangePropertyActive(action.propertyId, gwtConverter.convertOrCast(action.columnKey), action.focused,
                action.changeSelectionProps, changeSelectionColumnKeys, action.changeSelectionValues);
    }
}
