package platform.gwt.form2.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.form2.server.FormSessionObject;
import platform.gwt.form2.server.RemoteServiceImpl;
import platform.gwt.form2.server.convert.GwtToClientConverter;
import platform.gwt.form2.shared.actions.form.ChangeProperty;
import platform.gwt.form2.shared.actions.form.ServerResponseResult;

import java.io.IOException;

import static platform.base.BaseUtils.serializeObject;

public class ChangePropertyHandler extends ServerResponseActionHandler<ChangeProperty> {

    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ChangePropertyHandler(RemoteServiceImpl servlet) {
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
