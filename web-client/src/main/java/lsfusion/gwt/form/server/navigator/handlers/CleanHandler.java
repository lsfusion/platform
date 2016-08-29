package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.gwt.base.server.dispatch.NavigatorActionHandler;
import lsfusion.gwt.base.server.dispatch.SimpleActionHandlerEx;
import lsfusion.gwt.base.shared.actions.VoidResult;
import lsfusion.gwt.form.server.FormDispatchServlet;
import lsfusion.gwt.form.shared.actions.navigator.CleanAction;
import lsfusion.interop.RemoteLogicsInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class CleanHandler extends SimpleActionHandlerEx<CleanAction, VoidResult, RemoteLogicsInterface> implements NavigatorActionHandler {
    public CleanHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(CleanAction action, ExecutionContext context) throws DispatchException, IOException {
        servlet.invalidate();
        return new VoidResult();
    }
}
