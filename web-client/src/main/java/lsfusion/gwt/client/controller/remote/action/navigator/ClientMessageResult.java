package lsfusion.gwt.client.controller.remote.action.navigator;

import net.customware.gwt.dispatch.shared.Result;

import java.util.List;

public class ClientMessageResult implements Result {
    public List<Integer> notificationList;

    public ClientMessageResult() {
    }

    public ClientMessageResult(List<Integer> notificationList) {
        this.notificationList = notificationList;
    }
}
