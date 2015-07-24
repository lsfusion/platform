package lsfusion.gwt.form.server.form.handlers;

import lsfusion.base.BaseUtils;
import lsfusion.gwt.base.server.dispatch.NavigatorActionHandler;
import lsfusion.gwt.form.server.FormDispatchServlet;
import lsfusion.gwt.form.shared.actions.GetForm;
import lsfusion.gwt.form.shared.actions.GetFormResult;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class GetFormHandler extends FormActionHandler<GetForm, GetFormResult> implements NavigatorActionHandler {
    public GetFormHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GetFormResult executeEx(GetForm action, ExecutionContext context) throws DispatchException, IOException {
        if (BaseUtils.isRedundantString(action.sid)) {
            throw new IllegalStateException("Form's SID shouldn't be emptry");
        }

        return new GetFormResult(
                getFormSessionManager().createForm(
                        action.canonicalName,
                        action.sid,
                        servlet.getNavigator().createForm(action.sid, action.initialObjects, action.isModal, true),
                        null, null, servlet
                )
        );
    }
}

