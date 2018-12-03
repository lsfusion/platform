package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.client.logics.DeSerializer;
import lsfusion.client.navigator.ClientAbstractWindow;
import lsfusion.client.navigator.ClientNavigatorWindow;
import lsfusion.gwt.base.server.dispatch.NavigatorActionHandler;
import lsfusion.gwt.form.server.LSFusionDispatchServlet;
import lsfusion.gwt.form.server.convert.ClientNavigatorToGwtConverter;
import lsfusion.gwt.form.server.form.handlers.LoggableActionHandler;
import lsfusion.gwt.form.shared.actions.navigator.GetNavigatorInfo;
import lsfusion.gwt.form.shared.actions.navigator.GetNavigatorInfoResult;
import lsfusion.gwt.form.shared.view.GNavigatorElement;
import lsfusion.gwt.form.shared.view.window.GAbstractWindow;
import lsfusion.gwt.form.shared.view.window.GNavigatorWindow;
import lsfusion.interop.RemoteLogicsInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GetNavigatorInfoHandler extends LoggableActionHandler<GetNavigatorInfo, GetNavigatorInfoResult, RemoteLogicsInterface> implements NavigatorActionHandler {

    public GetNavigatorInfoHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public GetNavigatorInfoResult executeEx(GetNavigatorInfo action, ExecutionContext context) throws DispatchException, IOException {
        ClientNavigatorToGwtConverter converter = new ClientNavigatorToGwtConverter();

        DeSerializer.NavigatorData navigatorData = DeSerializer.deserializeListClientNavigatorElementWithChildren(servlet.getNavigator().getNavigatorTree());

        GNavigatorElement root = converter.convertOrCast(navigatorData.root);

        ArrayList<GNavigatorWindow> navigatorWindows = new ArrayList<>();
        for (ClientNavigatorWindow window : navigatorData.windows.values()) {
            GNavigatorWindow gWindow = converter.convertOrCast(window);
            navigatorWindows.add(gWindow);
        }

        //getting common windows
        List<ClientAbstractWindow> clientWindows = DeSerializer.deserializeListClientNavigatorWindow(servlet.getNavigator().getCommonWindows());
        List<GAbstractWindow> windows = new ArrayList<>();
        for (ClientAbstractWindow clientWindow : clientWindows) {
            windows.add((GAbstractWindow) converter.convertOrCast(clientWindow));
        }

        return new GetNavigatorInfoResult(root, navigatorWindows, windows);
    }
}
