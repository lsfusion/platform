package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.gwt.form.server.FormDispatchServlet;
import lsfusion.gwt.form.server.form.handlers.LoggableActionHandler;
import lsfusion.gwt.form.shared.actions.navigator.GenerateID;
import lsfusion.gwt.form.shared.actions.navigator.GenerateIDResult;
import lsfusion.interop.RemoteLogicsInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class GenerateIDHandler extends LoggableActionHandler<GenerateID, GenerateIDResult, RemoteLogicsInterface> {
    public GenerateIDHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GenerateIDResult executeEx(GenerateID action, ExecutionContext context) throws DispatchException, IOException {
        return new GenerateIDResult(servlet.getLogics().generateID());
    }
}
