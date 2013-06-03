package lsfusion.gwt.form.server.navigator.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.gwt.base.server.dispatch.SimpleActionHandlerEx;
import lsfusion.gwt.form.server.FormDispatchServlet;
import lsfusion.gwt.form.shared.actions.navigator.GenerateID;
import lsfusion.gwt.form.shared.actions.navigator.GenerateIDResult;
import lsfusion.interop.RemoteLogicsInterface;

import java.io.IOException;

public class GenerateIDHandler extends SimpleActionHandlerEx<GenerateID, GenerateIDResult, RemoteLogicsInterface> {
    public GenerateIDHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GenerateIDResult executeEx(GenerateID action, ExecutionContext context) throws DispatchException, IOException {
        return new GenerateIDResult(servlet.getLogics().generateID());
    }
}
