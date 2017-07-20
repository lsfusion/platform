package lsfusion.gwt.form.shared.actions.navigator;

import net.customware.gwt.dispatch.shared.Result;

import java.util.List;

public class ClientMessageResult implements Result {
    public String currentForm;
    public List<Integer> notificationList;

    public ClientMessageResult() {
    }

    public ClientMessageResult(String currentForm, List<Integer> notificationList) {
        this.currentForm = currentForm;
        this.notificationList = notificationList;
    }
}
