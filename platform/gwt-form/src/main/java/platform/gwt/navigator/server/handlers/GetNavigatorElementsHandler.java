package platform.gwt.navigator.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.client.logics.DeSerializer;
import platform.client.navigator.ClientNavigatorElement;
import platform.gwt.base.server.handlers.SimpleActionHandlerEx;
import platform.gwt.navigator.server.NavigatorServiceImpl;
import platform.gwt.navigator.shared.actions.GetNavigatorElements;
import platform.gwt.navigator.shared.actions.GetNavigatorElementsResult;

import java.io.IOException;

public class GetNavigatorElementsHandler extends SimpleActionHandlerEx<GetNavigatorElements, GetNavigatorElementsResult> {
    protected final NavigatorServiceImpl servlet;

    public GetNavigatorElementsHandler(NavigatorServiceImpl servlet) {
        this.servlet = servlet;
    }

    @Override
    public GetNavigatorElementsResult executeEx(GetNavigatorElements action, ExecutionContext context) throws DispatchException, IOException {
        DeSerializer.deserializeListClientNavigatorElementWithChildren(servlet.getNavigator().getNavigatorTree());

        return new GetNavigatorElementsResult(ClientNavigatorElement.root.getGwtElement());
    }
}
