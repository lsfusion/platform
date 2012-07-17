package platform.gwt.form2.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.client.logics.ClientGroupObjectValue;
import platform.gwt.base.server.FormSessionObject;
import platform.gwt.form2.server.RemoteServiceImpl;
import platform.gwt.form2.shared.actions.form.ChangeProperty;
import platform.gwt.form2.shared.actions.form.ServerResponseResult;

import java.io.IOException;

import static platform.base.BaseUtils.serializeObject;

public class ChangePropertyHandler extends ServerResponseActionHandler<ChangeProperty> {
    public ChangePropertyHandler(RemoteServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ChangeProperty action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(
                form,
                form.remoteForm.changeProperty(
                        action.requestIndex,
                        action.propertyId,
                        new ClientGroupObjectValue().serialize(),
                        //todo: convert color, etc...
                        serializeObject(action.value),
                        null
                )
        );
    }
}
