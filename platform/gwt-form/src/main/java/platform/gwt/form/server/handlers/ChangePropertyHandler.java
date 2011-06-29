package platform.gwt.form.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.base.DebugUtils;
import platform.client.logics.ClientGroupObjectValue;
import platform.gwt.base.shared.MessageException;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.server.RemoteFormServiceImpl;
import platform.gwt.form.shared.actions.form.ChangeProperty;
import platform.gwt.form.shared.actions.form.FormChangesResult;

import static platform.base.BaseUtils.serializeObject;

public class ChangePropertyHandler extends FormChangesActionHandler<ChangeProperty> {
    public ChangePropertyHandler(RemoteFormServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public FormChangesResult execute(ChangeProperty action, ExecutionContext context) throws DispatchException {
        try {
            FormSessionObject form = getSessionFormExceptionally(action.formSessionID);

            //пока пустой columnKey
            form.remoteForm.changePropertyDraw(action.propertyId, new ClientGroupObjectValue().serialize(), serializeObject(action.value.getValue()), false, false);

            return getRemoteChanges(form);
        } catch (Throwable e) {
            logger.error("Ошибка в changeGroupObject: ", e);
            e.printStackTrace();
            throw new MessageException(DebugUtils.getInitialCause(e).getMessage());
        }
    }
}
