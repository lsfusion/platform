package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.gwt.base.server.dispatch.NavigatorActionHandler;
import lsfusion.gwt.base.server.dispatch.SimpleActionHandlerEx;
import lsfusion.gwt.form.server.FormDispatchServlet;
import lsfusion.gwt.form.shared.actions.navigator.ClientMessage;
import lsfusion.gwt.form.shared.actions.navigator.ClientMessageResult;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.remote.CallbackMessage;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;
import java.util.List;

public class ClientMessageHandler extends SimpleActionHandlerEx<ClientMessage, ClientMessageResult, RemoteLogicsInterface> implements NavigatorActionHandler {
    public ClientMessageHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ClientMessageResult executeEx(ClientMessage action, ExecutionContext context) throws DispatchException, IOException {
        List<CallbackMessage> messages = servlet.getNavigator().getClientCallBack().pullMessages();
        return new ClientMessageResult(messages != null && messages.contains(CallbackMessage.SERVER_RESTARTING));
    }
}
