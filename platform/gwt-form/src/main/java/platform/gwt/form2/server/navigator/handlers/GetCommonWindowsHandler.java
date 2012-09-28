package platform.gwt.form2.server.navigator.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.client.logics.DeSerializer;
import platform.client.navigator.ClientAbstractWindow;
import platform.gwt.base.server.LogicsDispatchServlet;
import platform.gwt.base.server.handlers.SimpleActionHandlerEx;
import platform.gwt.form2.server.convert.ClientNavigatorToGwtConverter;
import platform.gwt.form2.shared.actions.navigator.GetCommonWindows;
import platform.gwt.form2.shared.actions.navigator.GetCommonWindowsResult;
import platform.gwt.form2.shared.view.window.GAbstractWindow;
import platform.interop.RemoteLogicsInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GetCommonWindowsHandler extends SimpleActionHandlerEx<GetCommonWindows, GetCommonWindowsResult, RemoteLogicsInterface> {
    public GetCommonWindowsHandler(LogicsDispatchServlet<RemoteLogicsInterface> servlet) {
        super(servlet);
    }

    @Override
    public GetCommonWindowsResult executeEx(GetCommonWindows action, ExecutionContext context) throws DispatchException, IOException {
        ClientNavigatorToGwtConverter converter = ClientNavigatorToGwtConverter.getInstance();
        List<ClientAbstractWindow> clientWindows = DeSerializer.deserializeListClientNavigatorWindow(servlet.getNavigator().getCommonWindows());
        List<GAbstractWindow> windows = new ArrayList<GAbstractWindow>();
        for (ClientAbstractWindow clientWindow : clientWindows) {
            windows.add((GAbstractWindow) converter.convertOrCast(clientWindow));
        }
        return new GetCommonWindowsResult(windows);
    }
}
