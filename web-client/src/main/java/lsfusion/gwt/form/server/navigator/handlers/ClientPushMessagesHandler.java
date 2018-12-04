package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.gwt.form.server.navigator.NavigatorActionHandler;
import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.shared.actions.navigator.ClientPushMessage;
import lsfusion.gwt.form.shared.actions.navigator.ClientMessageResult;
import lsfusion.interop.remote.LifecycleMessage;
import lsfusion.interop.remote.PushMessage;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientPushMessagesHandler extends NavigatorActionHandler<ClientPushMessage, ClientMessageResult> {

    public ClientPushMessagesHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ClientMessageResult executeEx(ClientPushMessage action, ExecutionContext context) throws DispatchException, IOException {
        List<LifecycleMessage> messages = servlet.getClientCallBack().pullMessages();
        return getClientMessageResult(messages);
    }

    @Override
    protected String getActionDetails(ClientPushMessage action) {
        return null; // too many logs
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
