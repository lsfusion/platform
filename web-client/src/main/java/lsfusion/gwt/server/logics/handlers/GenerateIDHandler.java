package lsfusion.gwt.server.logics.handlers;

import lsfusion.gwt.server.logics.LogicsActionHandler;
import lsfusion.gwt.server.LSFusionDispatchServlet;
import lsfusion.gwt.shared.form.actions.logics.GenerateID;
import lsfusion.gwt.shared.form.actions.logics.GenerateIDResult;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class GenerateIDHandler extends LogicsActionHandler<GenerateID, GenerateIDResult> {
    public GenerateIDHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GenerateIDResult executeEx(GenerateID action, ExecutionContext context) throws DispatchException, IOException {
        return new GenerateIDResult(getRemoteLogics(action).generateID());
    }
}
