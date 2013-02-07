package platform.gwt.form.server.navigator.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.client.logics.DeSerializer;
import platform.client.navigator.ClientAbstractWindow;
import platform.client.navigator.ClientNavigatorWindow;
import platform.gwt.base.server.handlers.SimpleActionHandlerEx;
import platform.gwt.form.server.RemoteServiceImpl;
import platform.gwt.form.server.convert.ClientNavigatorToGwtConverter;
import platform.gwt.form.server.convert.FileManager;
import platform.gwt.form.shared.actions.navigator.GetNavigatorInfo;
import platform.gwt.form.shared.actions.navigator.GetNavigatorInfoResult;
import platform.gwt.form.shared.view.GNavigatorElement;
import platform.gwt.form.shared.view.window.GAbstractWindow;
import platform.gwt.form.shared.view.window.GNavigatorWindow;
import platform.interop.RemoteLogicsInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GetNavigatorInfoHandler extends SimpleActionHandlerEx<GetNavigatorInfo, GetNavigatorInfoResult, RemoteLogicsInterface> {

    public GetNavigatorInfoHandler(RemoteServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public GetNavigatorInfoResult executeEx(GetNavigatorInfo action, ExecutionContext context) throws DispatchException, IOException {
        FileManager.initializeAppFolder(servlet.getServletContext().getRealPath(""));
        ClientNavigatorToGwtConverter converter = new ClientNavigatorToGwtConverter();

        DeSerializer.NavigatorData navigatorData = DeSerializer.deserializeListClientNavigatorElementWithChildren(servlet.getNavigator().getNavigatorTree());

        GNavigatorElement root = converter.convertOrCast(navigatorData.root);

        ArrayList<GNavigatorWindow> navigatorWindows = new ArrayList<GNavigatorWindow>();
        for (ClientNavigatorWindow window : navigatorData.windows.values()) {
            GNavigatorWindow gWindow = converter.convertOrCast(window);
            navigatorWindows.add(gWindow);
        }

        //getting common windows
        List<ClientAbstractWindow> clientWindows = DeSerializer.deserializeListClientNavigatorWindow(servlet.getNavigator().getCommonWindows());
        List<GAbstractWindow> windows = new ArrayList<GAbstractWindow>();
        for (ClientAbstractWindow clientWindow : clientWindows) {
            windows.add((GAbstractWindow) converter.convertOrCast(clientWindow));
        }

        return new GetNavigatorInfoResult(root, navigatorWindows, windows);
    }
}
