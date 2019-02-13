package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.navigator.NavigatorServerResponseActionHandler;
import lsfusion.gwt.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.shared.actions.navigator.ContinueNavigatorAction;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class ContinueNavigatorActionHandler extends NavigatorServerResponseActionHandler<ContinueNavigatorAction> {
    private final GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ContinueNavigatorActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ContinueNavigatorAction action, ExecutionContext context) throws DispatchException, IOException {
        Object actionResults[] = new Object[action.actionResults.length];
        for (int i = 0; i < actionResults.length; ++i) {
            actionResults[i] = gwtConverter.convertOrCast(action.actionResults[i]);
        }

        return getServerResponseResult(action, getRemoteNavigator(action).continueNavigatorAction(actionResults));
    }
}
