package lsfusion.gwt.client.controller.remote.action.navigator;

import net.customware.gwt.dispatch.shared.Result;

public class InitializeNavigatorResult implements Result {
    public ClientSettingsResult clientSettingsResult;
    public NavigatorInfoResult navigatorInfoResult;


    public InitializeNavigatorResult() {
    }

    public InitializeNavigatorResult(ClientSettingsResult clientSettingsResult, NavigatorInfoResult navigatorInfoResult) {
        this.clientSettingsResult = clientSettingsResult;
        this.navigatorInfoResult = navigatorInfoResult;
    }
}