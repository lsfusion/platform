package platform.gwt.form.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.base.DebugUtils;
import platform.gwt.base.shared.MessageException;
import platform.gwt.form.server.RemoteFormServiceImpl;
import platform.gwt.form.shared.actions.GetForm;
import platform.gwt.form.shared.actions.GetFormResult;
import platform.interop.form.RemoteFormInterface;

public class GetFormHandler extends FormActionHandler<GetForm> {
    public GetFormHandler(RemoteFormServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public GetFormResult execute(GetForm action, ExecutionContext context) throws DispatchException {
        String sid = action.sid != null && !action.sid.isEmpty()
                     ? action.sid
                     : "connectionsForm";

        try {
            RemoteFormInterface remoteForm = servlet.getNavigator().createForm(sid, false, true);
            return createResult(remoteForm);
        } catch (Throwable e) {
            logger.error("Ошибка в getForm: ", e);
            e.printStackTrace();
            throw new MessageException(DebugUtils.getInitialCause(e).getMessage());
        }
    }
}
