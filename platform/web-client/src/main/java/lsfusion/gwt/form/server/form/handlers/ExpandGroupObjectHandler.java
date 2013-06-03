package lsfusion.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.gwt.form.server.FormDispatchServlet;
import lsfusion.gwt.form.server.FormSessionObject;
import lsfusion.gwt.form.server.convert.GwtToClientConverter;
import lsfusion.gwt.form.shared.actions.form.ExpandGroupObject;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;

import java.io.IOException;

public class ExpandGroupObjectHandler extends ServerResponseActionHandler<ExpandGroupObject> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ExpandGroupObjectHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ExpandGroupObject action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);

        byte[] keyValues = gwtConverter.convertOrCast(action.value);

        return getServerResponseResult(form, form.remoteForm.expandGroupObject(action.requestIndex, action.groupObjectId, keyValues));
    }
}
