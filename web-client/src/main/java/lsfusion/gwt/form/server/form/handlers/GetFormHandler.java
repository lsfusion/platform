package lsfusion.gwt.form.server.form.handlers;

import lsfusion.base.BaseUtils;
import lsfusion.gwt.form.server.navigator.NavigatorActionHandler;
import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.shared.actions.navigator.GetForm;
import lsfusion.gwt.form.shared.actions.navigator.GetFormResult;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class GetFormHandler extends NavigatorActionHandler<GetForm, GetFormResult> {
    public GetFormHandler(LSFusionDispatchServlet servlet) {
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
                        servlet.getNavigator().createForm(action.sid, null, action.isModal, true),
                        null, null, action.tabSID, servlet
                )
        );
    }
}

