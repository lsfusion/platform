package lsfusion.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.gwt.form.server.FormDispatchServlet;
import lsfusion.gwt.form.server.FormSessionObject;
import lsfusion.gwt.form.server.convert.GwtToClientConverter;
import lsfusion.gwt.form.shared.actions.form.ChangeGroupObject;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;

import java.io.IOException;

public class ChangeGroupObjectHandler extends ServerResponseActionHandler<ChangeGroupObject> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ChangeGroupObjectHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ChangeGroupObject action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);

        byte[] keyValues = gwtConverter.convertOrCast(action.keyValues);

        return getServerResponseResult(form, form.remoteForm.changeGroupObject(action.requestIndex, action.groupId, keyValues));
    }
}
