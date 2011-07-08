package platform.gwt.form.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import platform.gwt.base.server.FormSessionObject;
import platform.gwt.base.shared.MessageException;
import platform.gwt.form.server.RemoteFormServiceImpl;
import platform.gwt.form.shared.actions.GetFormResult;
import platform.gwt.form.shared.actions.form.CreateEditorForm;
import platform.interop.form.RemoteDialogInterface;

import java.io.IOException;

public class CreateEditorFormHandler extends FormActionHandler<CreateEditorForm> {
    public CreateEditorFormHandler(RemoteFormServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public GetFormResult executeEx(CreateEditorForm action, ExecutionContext context) throws IOException, MessageException {
        FormSessionObject form = getSessionFormExceptionally(action.formSessionID);

        //пока пустой columnKey
        RemoteDialogInterface remoteForm = form.remoteForm.createEditorPropertyDialog(action.propertyId);

        return createResult(remoteForm);
    }
}
