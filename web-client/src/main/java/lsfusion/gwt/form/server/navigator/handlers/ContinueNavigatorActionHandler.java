package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.gwt.base.server.dispatch.NavigatorActionHandler;
import lsfusion.gwt.form.server.FormDispatchServlet;
import lsfusion.gwt.form.server.convert.GwtToClientConverter;
import lsfusion.gwt.form.server.form.handlers.ServerResponseActionHandler;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.form.shared.actions.navigator.ContinueNavigatorAction;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class ContinueNavigatorActionHandler extends ServerResponseActionHandler<ContinueNavigatorAction> implements NavigatorActionHandler {
    private final GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ContinueNavigatorActionHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ContinueNavigatorAction action, ExecutionContext context) throws DispatchException, IOException {
        Object actionResults[] = new Object[action.actionResults.length];
        for (int i = 0; i < actionResults.length; ++i) {
            actionResults[i] = gwtConverter.convertOrCast(action.actionResults[i], servlet.getBLProvider());
        }

        return getServerResponseResult(servlet.getNavigator().continueNavigatorAction(actionResults));
    }
}
