package lsfusion.gwt.client.controller.remote.action.navigator;

import lsfusion.gwt.client.form.object.table.grid.user.design.GColorPreferences;
import lsfusion.gwt.client.view.GColorTheme;
import net.customware.gwt.dispatch.shared.Result;

public class GetClientSettingsResult implements Result {
    public boolean busyDialog;
    public long busyDialogTimeout;
    public boolean devMode;
    public boolean showDetailedInfo;
    public boolean forbidDuplicateForms;
    public boolean showNotDefinedStrings;
    public GColorTheme colorTheme;
    public GColorPreferences colorPreferences;

    public GetClientSettingsResult() {
    }

    public GetClientSettingsResult(boolean busyDialog, long busyDialogTimeout, boolean devMode, boolean showDetailedInfo,
                                   boolean forbidDuplicateForms, boolean showNotDefinedStrings, GColorTheme colorTheme, 
                                   GColorPreferences colorPreferences) {
        this.busyDialog = busyDialog;
        this.busyDialogTimeout = busyDialogTimeout;
        this.devMode = devMode;
        this.showDetailedInfo = showDetailedInfo;
        this.forbidDuplicateForms = forbidDuplicateForms;
        this.showNotDefinedStrings = showNotDefinedStrings;
        this.colorTheme = colorTheme;
        this.colorPreferences = colorPreferences;
    }
}