package platform.gwt.form2.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.base.BaseUtils;
import platform.gwt.form2.server.RemoteServiceImpl;
import platform.gwt.form2.shared.actions.GetForm;
import platform.gwt.form2.shared.actions.GetFormResult;

import java.io.IOException;

public class GetFormHandler extends FormActionHandler<GetForm, GetFormResult> {
    public GetFormHandler(RemoteServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public GetFormResult executeEx(GetForm action, ExecutionContext context) throws DispatchException, IOException {
        if (BaseUtils.isRedundantString(action.sid)) {
            throw new IllegalStateException("Form's SID shouldn't be emptry");
        }

        return new GetFormResult(
                getFormSessionManager().createForm(
                        servlet.getNavigator().createForm(action.sid, action.initialObjects, action.isModal, true)
                )
        );
    }
}

