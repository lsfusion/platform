package platform.gwt.form.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.client.logics.ClientGroupObjectValue;
import platform.gwt.base.server.FormSessionObject;
import platform.gwt.form.server.RemoteFormServiceImpl;
import platform.gwt.form.shared.actions.form.ChangeProperty;
import platform.gwt.form.shared.actions.form.FormChangesResult;

import java.io.IOException;

import static platform.base.BaseUtils.serializeObject;

public class ChangePropertyHandler extends FormChangesActionHandler<ChangeProperty> {
    public ChangePropertyHandler(RemoteFormServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public FormChangesResult executeEx(ChangeProperty action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);

        //пока пустой columnKey
        form.remoteForm.changePropertyDraw(action.propertyId, new ClientGroupObjectValue().serialize(), serializeObject(action.value.getValue()), false, false);

        return getRemoteChanges(form);
    }
}
