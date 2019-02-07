package lsfusion.gwt.shared.actions.navigator;

import net.customware.gwt.dispatch.shared.Result;

public class GetClientSettingsResult implements Result {
    public boolean busyDialog;
    public long busyDialogTimeout;
    public boolean configurationAccessAllowed;
    public boolean forbidDuplicateForms;

    public GetClientSettingsResult() {
    }

    public GetClientSettingsResult(boolean busyDialog, long busyDialogTimeout, boolean configurationAccessAllowed, boolean forbidDuplicateForms) {
        this.busyDialog = busyDialog;
        this.busyDialogTimeout = busyDialogTimeout;
        this.configurationAccessAllowed = configurationAccessAllowed;
        this.forbidDuplicateForms = forbidDuplicateForms;
    }
}