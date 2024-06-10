package lsfusion.gwt.client.controller.remote.action.navigator;

import lsfusion.gwt.client.base.result.VoidResult;

public class UpdateServiceClientInfoAction extends NavigatorRequestCountingAction<VoidResult> {
    public String subscription;
    public String clientId;

    public UpdateServiceClientInfoAction() {
    }

    public UpdateServiceClientInfoAction(String subscription, String clientId) {
        this.subscription = subscription;
        this.clientId = clientId;
    }
}
