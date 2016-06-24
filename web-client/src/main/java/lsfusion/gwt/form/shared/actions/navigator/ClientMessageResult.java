package lsfusion.gwt.form.shared.actions.navigator;

import net.customware.gwt.dispatch.shared.Result;

import java.util.List;

public class ClientMessageResult implements Result {
    public boolean restart;
    public String currentForm;
    public List<Integer> notificationList;

    public ClientMessageResult() {
    }

    public ClientMessageResult(boolean restart, String currentForm, List<Integer> notificationList) {
        this.restart = restart;
        this.currentForm = currentForm;
        this.notificationList = notificationList;
    }
}
