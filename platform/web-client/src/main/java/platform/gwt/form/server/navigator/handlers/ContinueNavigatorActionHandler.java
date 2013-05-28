package platform.gwt.form.server.navigator.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.form.server.FormDispatchServlet;
import platform.gwt.form.server.convert.GwtToClientConverter;
import platform.gwt.form.server.form.handlers.ServerResponseActionHandler;
import platform.gwt.form.shared.actions.form.ServerResponseResult;
import platform.gwt.form.shared.actions.navigator.ContinueNavigatorAction;

import java.io.IOException;

public class ContinueNavigatorActionHandler extends ServerResponseActionHandler<ContinueNavigatorAction> {
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
