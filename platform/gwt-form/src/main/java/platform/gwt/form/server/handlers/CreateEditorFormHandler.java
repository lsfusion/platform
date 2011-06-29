package platform.gwt.form.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.base.DebugUtils;
import platform.gwt.base.shared.MessageException;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.server.RemoteFormServiceImpl;
import platform.gwt.form.shared.actions.GetFormResult;
import platform.gwt.form.shared.actions.form.CreateEditorForm;
import platform.interop.form.RemoteDialogInterface;

public class CreateEditorFormHandler extends FormActionHandler<CreateEditorForm> {
    public CreateEditorFormHandler(RemoteFormServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public GetFormResult execute(CreateEditorForm action, ExecutionContext context) throws DispatchException {
        try {
            FormSessionObject form = getSessionFormExceptionally(action.formSessionID);

            //пока пустой columnKey
            RemoteDialogInterface remoteForm = form.remoteForm.createEditorPropertyDialog(action.propertyId);

            return createResult(remoteForm);
        } catch (Throwable e) {
            logger.error("Ошибка в changeGroupObject: ", e);
            e.printStackTrace();
            throw new MessageException(DebugUtils.getInitialCause(e).getMessage());
        }
    }
}
