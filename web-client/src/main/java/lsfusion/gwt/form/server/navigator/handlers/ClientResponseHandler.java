package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.gwt.base.server.dispatch.NavigatorActionHandler;
import lsfusion.gwt.base.server.dispatch.SimpleActionHandlerEx;
import lsfusion.gwt.form.server.FormDispatchServlet;
import lsfusion.gwt.form.shared.actions.navigator.ClientResponse;
import lsfusion.gwt.form.shared.actions.navigator.ClientResponseResult;
import lsfusion.interop.RemoteLogicsInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class ClientResponseHandler extends SimpleActionHandlerEx<ClientResponse, ClientResponseResult, RemoteLogicsInterface> implements NavigatorActionHandler{
    public ClientResponseHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ClientResponseResult executeEx(ClientResponse action, ExecutionContext context) throws DispatchException, IOException {
        servlet.getNavigator().getClientCallBack().denyRestart();
        return new ClientResponseResult();
    }
}
