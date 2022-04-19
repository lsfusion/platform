package lsfusion.gwt.client.controller.remote.action.navigator;

import lsfusion.gwt.client.form.object.table.grid.user.design.GColorPreferences;
import lsfusion.gwt.client.view.GColorTheme;
import net.customware.gwt.dispatch.shared.Result;

public class GetClientSettingsResult implements Result {
    public boolean busyDialog;
    public long busyDialogTimeout;
    public boolean devMode;
    public String projectLSFDir;
    public boolean showDetailedInfo;
    public boolean forbidDuplicateForms;
    public boolean showNotDefinedStrings;
    public boolean pivotOnlySelectedColumn;
    public GColorTheme colorTheme;
    public GColorPreferences colorPreferences;
    public String dateFormat;
    public String timeFormat;
    public String staticImagesURL;

    public GetClientSettingsResult() {
    }

    public GetClientSettingsResult(boolean busyDialog, long busyDialogTimeout, boolean devMode, String projectLSFDir,
                                   boolean showDetailedInfo, boolean forbidDuplicateForms, boolean showNotDefinedStrings, boolean pivotOnlySelectedColumn,
                                   GColorTheme colorTheme, GColorPreferences colorPreferences, String dateFormat, String timeFormat, String staticImagesURL) {
        this.busyDialog = busyDialog;
        this.busyDialogTimeout = busyDialogTimeout;
        this.devMode = devMode;
        this.projectLSFDir = projectLSFDir;
        this.showDetailedInfo = showDetailedInfo;
        this.forbidDuplicateForms = forbidDuplicateForms;
        this.showNotDefinedStrings = showNotDefinedStrings;
        this.pivotOnlySelectedColumn = pivotOnlySelectedColumn;
        this.colorTheme = colorTheme;
        this.colorPreferences = colorPreferences;
        this.dateFormat = dateFormat;
        this.timeFormat = timeFormat;
        this.staticImagesURL = staticImagesURL;
    }
}