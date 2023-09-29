package lsfusion.gwt.client.controller.remote.action.navigator;

import net.customware.gwt.dispatch.shared.Result;

import java.util.Map;

public class InitializeNavigatorResult implements Result {
    public GClientSettings gClientSettings;
    public NavigatorInfo navigatorInfo;
    public Map<String, String> lsfParamsAPIKeys;

    public InitializeNavigatorResult() {
    }

    public InitializeNavigatorResult(GClientSettings gClientSettings, NavigatorInfo navigatorInfo, Map<String, String> lsfParamsAPIKeys) {
        this.gClientSettings = gClientSettings;
        this.navigatorInfo = navigatorInfo;
        this.lsfParamsAPIKeys = lsfParamsAPIKeys;
    }
}