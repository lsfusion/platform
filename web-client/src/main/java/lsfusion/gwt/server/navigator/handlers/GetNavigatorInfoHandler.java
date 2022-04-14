package lsfusion.gwt.server.navigator.handlers;

import com.google.common.base.Throwables;
import lsfusion.client.navigator.NavigatorData;
import lsfusion.client.navigator.window.ClientNavigatorWindow;
import lsfusion.gwt.client.controller.remote.action.navigator.GetNavigatorInfo;
import lsfusion.gwt.client.controller.remote.action.navigator.GetNavigatorInfoResult;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.ClientNavigatorToGwtConverter;
import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class GetNavigatorInfoHandler extends NavigatorActionHandler<GetNavigatorInfo, GetNavigatorInfoResult> {

    public GetNavigatorInfoHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GetNavigatorInfoResult executeEx(GetNavigatorInfo action, ExecutionContext context) throws RemoteException {
        ClientNavigatorToGwtConverter converter = new ClientNavigatorToGwtConverter(servlet.getServletContext(), getLogicsName(action));

        RemoteNavigatorInterface remoteNavigator = getRemoteNavigator(action);
        NavigatorData navigatorData = null;
        byte[] navigatorTree = remoteNavigator.getNavigatorTree();
        try {
            navigatorData = NavigatorData.deserializeListClientNavigatorElementWithChildren(navigatorTree);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        GNavigatorElement root = converter.convertOrCast(navigatorData.root);

        ArrayList<GNavigatorWindow> navigatorWindows = new ArrayList<>();
        for (ClientNavigatorWindow window : navigatorData.windows.values()) {
            GNavigatorWindow gWindow = converter.convertOrCast(window);
            navigatorWindows.add(gWindow);
        }

        //getting common windows
        List<GAbstractWindow> windows = new ArrayList<>();
        windows.add((GAbstractWindow) converter.convertOrCast(navigatorData.logs));
        windows.add((GAbstractWindow) converter.convertOrCast(navigatorData.status));
        windows.add((GAbstractWindow) converter.convertOrCast(navigatorData.forms));

        return new GetNavigatorInfoResult(root, navigatorWindows, windows);
    }
}
