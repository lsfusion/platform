package lsfusion.gwt.form.shared.actions.navigator;

import net.customware.gwt.dispatch.shared.Result;

public class GetSecuritySettingsResult implements Result {
    public boolean devMode;
    public boolean configurationAccessAllowed;

    public GetSecuritySettingsResult() {
    }

    public GetSecuritySettingsResult(boolean devMode, boolean configurationAccessAllowed) {
        this.devMode = devMode;
        this.configurationAccessAllowed = configurationAccessAllowed;
    }
}