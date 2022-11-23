package lsfusion.gwt.client.controller.remote.action.navigator;

import net.customware.gwt.dispatch.shared.Result;

public class InitializeNavigatorResult implements Result {
    public WebClientSettings webClientSettings;
    public NavigatorInfo navigatorInfo;

    public InitializeNavigatorResult() {
    }

    public InitializeNavigatorResult(WebClientSettings webClientSettings, NavigatorInfo navigatorInfo) {
        this.webClientSettings = webClientSettings;
        this.navigatorInfo = navigatorInfo;
    }
}