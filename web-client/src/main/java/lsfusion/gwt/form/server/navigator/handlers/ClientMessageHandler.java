package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.gwt.base.server.dispatch.NavigatorActionHandler;
import lsfusion.gwt.base.server.dispatch.SimpleActionHandlerEx;
import lsfusion.gwt.form.server.FormDispatchServlet;
import lsfusion.gwt.form.shared.actions.navigator.ClientMessage;
import lsfusion.gwt.form.shared.actions.navigator.ClientMessageResult;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.remote.ClientCallBackInterface;
import lsfusion.interop.remote.LifecycleMessage;
import lsfusion.interop.remote.PushMessage;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientMessageHandler extends SimpleActionHandlerEx<ClientMessage, ClientMessageResult, RemoteLogicsInterface> implements NavigatorActionHandler {

    public ClientMessageHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ClientMessageResult executeEx(ClientMessage action, ExecutionContext context) throws DispatchException, IOException {
        List<LifecycleMessage> messages = servlet.getClientCallBack().pullMessages();
        return getClientMessageResult(messages);
    }

    private ClientMessageResult getClientMessageResult(List<LifecycleMessage> messages) throws IOException {
        String currentForm = null;
        List<Integer> notificationList = new ArrayList<>();
        if(messages != null) {
            currentForm = servlet.getNavigator().getCurrentForm();
            for (LifecycleMessage message : messages) {
                if(message instanceof PushMessage) {
                    notificationList.add(((PushMessage) message).idNotification);
                }
            }
        }
        return new ClientMessageResult(currentForm, notificationList);
    }
}
