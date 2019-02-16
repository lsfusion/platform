package lsfusion.gwt.server.logics.handlers;

import lsfusion.gwt.server.logics.LogicsActionHandler;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.shared.actions.logics.GenerateID;
import lsfusion.gwt.shared.actions.logics.GenerateIDResult;
import lsfusion.http.provider.logics.LogicsRunnable;
import lsfusion.http.provider.logics.LogicsSessionObject;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class GenerateIDHandler extends LogicsActionHandler<GenerateID, GenerateIDResult> {
    public GenerateIDHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GenerateIDResult executeEx(GenerateID action, ExecutionContext context) throws DispatchException, IOException {
        return runRequest(action, new LogicsRunnable<GenerateIDResult>() {
            public GenerateIDResult run(LogicsSessionObject sessionObject) throws IOException {
                return new GenerateIDResult(sessionObject.remoteLogics.generateID());
            }
        });
    }
}
