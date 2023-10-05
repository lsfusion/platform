package lsfusion.gwt.client.controller.remote.action.navigator;

import net.customware.gwt.dispatch.shared.Result;

public class InitializeNavigatorResult implements Result {
    public GClientSettings gClientSettings;
    public NavigatorInfo navigatorInfo;

    public InitializeNavigatorResult() {
    }

    public InitializeNavigatorResult(GClientSettings gClientSettings, NavigatorInfo navigatorInfo) {
        this.gClientSettings = gClientSettings;
        this.navigatorInfo = navigatorInfo;
    }
}