package lsfusion.gwt.form.server.form.handlers;

import lsfusion.base.DefaultFormsType;
import lsfusion.gwt.base.server.LogicsAwareDispatchServlet;
import lsfusion.gwt.base.server.dispatch.SimpleActionHandlerEx;
import lsfusion.gwt.form.shared.actions.navigator.ShowDefaultFormsAction;
import lsfusion.gwt.form.shared.actions.navigator.ShowDefaultFormsResult;
import lsfusion.gwt.form.shared.view.GDefaultFormsType;
import lsfusion.interop.RemoteLogicsInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class ShowDefaultFormsHandler extends SimpleActionHandlerEx<ShowDefaultFormsAction, ShowDefaultFormsResult, RemoteLogicsInterface> {
    public ShowDefaultFormsHandler(LogicsAwareDispatchServlet<RemoteLogicsInterface> servlet) {
        super(servlet);
    }

    @Override
    public ShowDefaultFormsResult executeEx(ShowDefaultFormsAction action, ExecutionContext context) throws DispatchException, IOException {
        DefaultFormsType dft = servlet.getNavigator().showDefaultForms();
        return new ShowDefaultFormsResult(dft == null ? null : GDefaultFormsType.valueOf(dft.name()));
    }
}
