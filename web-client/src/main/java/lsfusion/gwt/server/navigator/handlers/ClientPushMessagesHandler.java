package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.shared.actions.navigator.ClientPushMessage;
import lsfusion.gwt.shared.actions.navigator.ClientMessageResult;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.remote.LifecycleMessage;
import lsfusion.interop.remote.PushMessage;
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
        return getClientMessageResult(getRemoteNavigator(action), messages);
    }

    @Override
    protected String getActionDetails(ClientPushMessage action) {
        return null; // too many logs
    }

    private ClientMessageResult getClientMessageResult(RemoteNavigatorInterface remoteNavigator, List<LifecycleMessage> messages) throws RemoteException {
        String currentForm = null;
        List<Integer> notificationList = new ArrayList<>();
        if(messages != null) {
            currentForm = remoteNavigator.getCurrentForm();
            for (LifecycleMessage message : messages) {
                if(message instanceof PushMessage) {
                    notificationList.add(((PushMessage) message).idNotification);
                }
            }
        }
        return new ClientMessageResult(currentForm, notificationList);
    }
}
