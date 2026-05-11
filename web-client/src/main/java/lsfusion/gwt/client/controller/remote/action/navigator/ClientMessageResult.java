package lsfusion.gwt.client.controller.remote.action.navigator;

import net.customware.gwt.dispatch.shared.Result;

import java.util.List;

public class ClientMessageResult implements Result {
    public List<ClientNotificationItem> notificationList;

    public ClientMessageResult() {
    }

    public ClientMessageResult(List<ClientNotificationItem> notificationList) {
        this.notificationList = notificationList;
    }
}
