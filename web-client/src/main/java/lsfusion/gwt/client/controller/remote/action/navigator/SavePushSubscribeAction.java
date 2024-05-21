package lsfusion.gwt.client.controller.remote.action.navigator;

import lsfusion.gwt.client.base.result.VoidResult;

public class SavePushSubscribeAction extends NavigatorRequestAction<VoidResult> {
    public String subscription ;

    public SavePushSubscribeAction() {
    }

    public SavePushSubscribeAction(String subscription) {
        this.subscription = subscription;
    }
}
