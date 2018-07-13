package lsfusion.gwt.form.shared.actions.navigator;

import net.customware.gwt.dispatch.shared.Result;

public class GetClientSettingsResult implements Result {
    public boolean busyDialog;
    public long busyDialogTimeout;

    public GetClientSettingsResult() {
    }

    public GetClientSettingsResult(boolean busyDialog, long busyDialogTimeout) {
        this.busyDialog = busyDialog;
        this.busyDialogTimeout = busyDialogTimeout;
    }
}