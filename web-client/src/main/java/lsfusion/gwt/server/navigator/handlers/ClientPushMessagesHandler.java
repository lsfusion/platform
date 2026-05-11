package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.client.controller.remote.action.navigator.ClientMessageResult;
import lsfusion.gwt.client.controller.remote.action.navigator.ClientNotificationItem;
import lsfusion.gwt.client.controller.remote.action.navigator.ClientPushMessage;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import lsfusion.interop.navigator.LifecycleMessage;
import lsfusion.interop.navigator.PushMessage;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class ClientPushMessagesHandler extends NavigatorActionHandler<ClientPushMessage, ClientMessageResult> {

    public ClientPushMessagesHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ClientMessageResult executeEx(ClientPushMessage action, ExecutionContext context) throws RemoteException {
        List<LifecycleMessage> messages = getClientCallback(action).pullMessages();
        return getClientMessageResult(messages);
    }

    @Override
    protected String getActionDetails(ClientPushMessage action) {
        return null; // too many logs
    }

    private ClientMessageResult getClientMessageResult(List<LifecycleMessage> messages) throws RemoteException {
        List<ClientNotificationItem> notificationList = new ArrayList<>();
        if(messages != null) {
            for (LifecycleMessage message : messages) {
                if(message instanceof PushMessage) {
                    PushMessage pm = (PushMessage) message;
                    notificationList.add(new ClientNotificationItem(pm.idNotification, pm.delay, pm.period));
                }
            }
        }
        return new ClientMessageResult(notificationList);
    }
}
