package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.gwt.base.server.dispatch.NavigatorActionHandler;
import lsfusion.gwt.base.server.dispatch.SimpleActionHandlerEx;
import lsfusion.gwt.form.server.FormDispatchServlet;
import lsfusion.gwt.form.shared.actions.navigator.ClientMessage;
import lsfusion.gwt.form.shared.actions.navigator.ClientMessageResult;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.remote.CallbackMessage;
import lsfusion.interop.remote.ClientCallbackMessage;
import lsfusion.interop.remote.LifecycleMessage;
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
        List<LifecycleMessage> messages = servlet.getNavigator().getClientCallBack().pullMessages();
        return new ClientMessageResult(serverRestarting(messages));
    }

    private boolean serverRestarting(List<LifecycleMessage> messages) {
        boolean result = false;
        if(messages != null) {
            for (LifecycleMessage message : messages) {
                if (message instanceof ClientCallbackMessage && ((ClientCallbackMessage) message).message.equals(CallbackMessage.SERVER_RESTARTING))
                    result = true;
            }
        }
        return result;
    }
}
