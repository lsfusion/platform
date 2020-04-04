package lsfusion.gwt.client.controller.remote.action.navigator;

import lsfusion.gwt.client.view.GColorTheme;
import net.customware.gwt.dispatch.shared.Result;

public class GetClientSettingsResult implements Result {
    public boolean busyDialog;
    public long busyDialogTimeout;
    public boolean devMode;
    public boolean showDetailedInfo;
    public boolean forbidDuplicateForms;
    public GColorTheme colorTheme;

    public GetClientSettingsResult() {
    }

    public GetClientSettingsResult(boolean busyDialog, long busyDialogTimeout, boolean devMode, boolean showDetailedInfo,
                                   boolean forbidDuplicateForms, GColorTheme colorTheme) {
        this.busyDialog = busyDialog;
        this.busyDialogTimeout = busyDialogTimeout;
        this.devMode = devMode;
        this.showDetailedInfo = showDetailedInfo;
        this.forbidDuplicateForms = forbidDuplicateForms;
        this.colorTheme = colorTheme;
    }
}