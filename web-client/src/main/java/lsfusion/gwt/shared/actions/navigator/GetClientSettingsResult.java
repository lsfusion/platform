package lsfusion.gwt.shared.actions.navigator;

import net.customware.gwt.dispatch.shared.Result;

public class GetClientSettingsResult implements Result {
    public boolean busyDialog;
    public long busyDialogTimeout;
    public boolean configurationAccessAllowed;

    public GetClientSettingsResult() {
    }

    public GetClientSettingsResult(boolean busyDialog, long busyDialogTimeout, boolean configurationAccessAllowed) {
        this.busyDialog = busyDialog;
        this.busyDialogTimeout = busyDialogTimeout;
        this.configurationAccessAllowed = configurationAccessAllowed;
    }
}