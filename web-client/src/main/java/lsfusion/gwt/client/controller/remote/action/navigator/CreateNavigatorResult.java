package lsfusion.gwt.client.controller.remote.action.navigator;

import net.customware.gwt.dispatch.shared.Result;

public class CreateNavigatorResult implements Result {
    public String sessionId;
    public Boolean isMobile;

    public CreateNavigatorResult() {
    }

    public CreateNavigatorResult(String sessionId, Boolean isMobile) {
        this.sessionId = sessionId;
        this.isMobile = isMobile;
    }
}