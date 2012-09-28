package platform.gwt.form2.server.navigator.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.client.logics.DeSerializer;
import platform.client.navigator.ClientNavigatorElement;
import platform.client.navigator.ClientNavigatorWindow;
import platform.gwt.base.server.handlers.SimpleActionHandlerEx;
import platform.gwt.form2.server.RemoteServiceImpl;
import platform.gwt.form2.server.convert.ClientNavigatorToGwtConverter;
import platform.gwt.form2.shared.actions.navigator.GetNavigatorElements;
import platform.gwt.form2.shared.actions.navigator.GetNavigatorElementsResult;
import platform.gwt.form2.shared.view.GNavigatorElement;
import platform.gwt.form2.shared.view.window.GNavigatorWindow;
import platform.interop.RemoteLogicsInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GetNavigatorElementsHandler extends SimpleActionHandlerEx<GetNavigatorElements, GetNavigatorElementsResult, RemoteLogicsInterface> {
    private final static ClientNavigatorToGwtConverter converter = ClientNavigatorToGwtConverter.getInstance();

    public GetNavigatorElementsHandler(RemoteServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public GetNavigatorElementsResult executeEx(GetNavigatorElements action, ExecutionContext context) throws DispatchException, IOException {
        DeSerializer.deserializeListClientNavigatorElementWithChildren(servlet.getNavigator().getNavigatorTree());

        GNavigatorElement root = converter.convertOrCast(ClientNavigatorElement.root);
        List<GNavigatorWindow> navigatorWindows = new ArrayList<GNavigatorWindow>();
        for (String sid : ClientNavigatorWindow.sidToWindow.keySet()) {
            GNavigatorWindow window = converter.convertOrCast(ClientNavigatorWindow.sidToWindow.get(sid));
            navigatorWindows.add(window);
        }
        return new GetNavigatorElementsResult(root, navigatorWindows.toArray(new GNavigatorWindow[navigatorWindows.size()]));
    }
}
