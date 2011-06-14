package platform.gwt.form.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.base.DebugUtils;
import platform.gwt.base.shared.MessageException;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.server.RemoteFormServiceImpl;
import platform.gwt.form.shared.actions.form.ApplyChanges;
import platform.gwt.form.shared.actions.form.FormChangesResult;

public class ApplyChangesHandler extends FormChangesActionHandler<ApplyChanges> {
    public ApplyChangesHandler(RemoteFormServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public FormChangesResult execute(ApplyChanges action, ExecutionContext context) throws DispatchException {
        try {
            FormSessionObject form = getSessionFormExceptionally(action.formSessionID);

            form.remoteForm.applyChanges();

            return getRemoteChanges(form);
        } catch (Throwable e) {
            logger.error("Ошибка в getRemoteChanges: ", e);
            e.printStackTrace();
            throw new MessageException(DebugUtils.getInitialCause(e).getMessage());
        }
    }
}
