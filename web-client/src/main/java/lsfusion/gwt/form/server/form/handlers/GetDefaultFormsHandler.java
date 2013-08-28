package lsfusion.gwt.form.server.form.handlers;

import lsfusion.gwt.base.server.LogicsAwareDispatchServlet;
import lsfusion.gwt.base.server.dispatch.SimpleActionHandlerEx;
import lsfusion.gwt.form.shared.actions.navigator.GetDefaultFormsAction;
import lsfusion.gwt.form.shared.actions.navigator.GetDefaultFormsResult;
import lsfusion.interop.RemoteLogicsInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;
import java.util.ArrayList;

public class GetDefaultFormsHandler extends SimpleActionHandlerEx<GetDefaultFormsAction, GetDefaultFormsResult, RemoteLogicsInterface> {
    public GetDefaultFormsHandler(LogicsAwareDispatchServlet<RemoteLogicsInterface> servlet) {
        super(servlet);
    }

    @Override
    public GetDefaultFormsResult executeEx(GetDefaultFormsAction action, ExecutionContext context) throws DispatchException, IOException {
        return new GetDefaultFormsResult((ArrayList<String>) servlet.getNavigator().getDefaultForms());
    }
}
