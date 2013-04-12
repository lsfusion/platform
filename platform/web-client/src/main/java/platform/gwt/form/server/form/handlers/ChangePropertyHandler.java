package platform.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.form.server.FormDispatchServlet;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.server.convert.GwtToClientConverter;
import platform.gwt.form.shared.actions.form.ChangeProperty;
import platform.gwt.form.shared.actions.form.ServerResponseResult;

import java.io.IOException;

import static platform.base.BaseUtils.serializeObject;

public class ChangePropertyHandler extends ServerResponseActionHandler<ChangeProperty> {

    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ChangePropertyHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ChangeProperty action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        Object value = gwtConverter.convertOrCast(action.value);
        byte[] fullKey = gwtConverter.convertOrCast(action.fullKey);
        return getServerResponseResult(
                form,
                form.remoteForm.changeProperty(
                        action.requestIndex,
                        action.propertyId,
                        fullKey,
                        serializeObject(value),
                        action.addedObjectId
                )
        );
    }
}
