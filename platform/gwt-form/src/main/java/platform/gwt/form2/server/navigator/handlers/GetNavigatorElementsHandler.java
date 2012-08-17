package platform.gwt.form2.server.navigator.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.client.logics.DeSerializer;
import platform.client.navigator.ClientNavigatorElement;
import platform.gwt.form2.server.convert.ClientNavigatorToGwtConverter;
import platform.gwt.base.server.handlers.SimpleActionHandlerEx;
import platform.gwt.form2.server.RemoteServiceImpl;
import platform.gwt.form2.shared.actions.navigator.GetNavigatorElements;
import platform.gwt.form2.shared.actions.navigator.GetNavigatorElementsResult;
import platform.gwt.form2.shared.view.GNavigatorElement;
import platform.interop.RemoteLogicsInterface;

import java.io.IOException;

public class GetNavigatorElementsHandler extends SimpleActionHandlerEx<GetNavigatorElements, GetNavigatorElementsResult, RemoteLogicsInterface> {
    private final static ClientNavigatorToGwtConverter converter = ClientNavigatorToGwtConverter.getInstance();

    public GetNavigatorElementsHandler(RemoteServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public GetNavigatorElementsResult executeEx(GetNavigatorElements action, ExecutionContext context) throws DispatchException, IOException {
        DeSerializer.deserializeListClientNavigatorElementWithChildren(servlet.getNavigator().getNavigatorTree());

        GNavigatorElement root = converter.convertOrCast(ClientNavigatorElement.root);
        return new GetNavigatorElementsResult(root);
    }
}
