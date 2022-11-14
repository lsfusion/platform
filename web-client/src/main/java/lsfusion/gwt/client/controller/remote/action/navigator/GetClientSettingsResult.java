package lsfusion.gwt.client.controller.remote.action.navigator;

import lsfusion.gwt.client.form.object.table.grid.user.design.GColorPreferences;
import lsfusion.gwt.client.view.GColorTheme;
import net.customware.gwt.dispatch.shared.Result;

import java.util.Map;

public class GetClientSettingsResult implements Result {
    public long busyDialogTimeout;
    public boolean devMode;
    public String projectLSFDir;
    public boolean showDetailedInfo;
    public boolean forbidDuplicateForms;
    public boolean showNotDefinedStrings;
    public boolean pivotOnlySelectedColumn;
    public String matchSearchSeparator;
    public GColorTheme colorTheme;
    public Map<String, String> versionedColorThemesCss;
    public GColorPreferences colorPreferences;
    public String dateFormat;
    public String timeFormat;
    public String staticImagesURL;
    public String[] preDefinedDateRangesNames;
    public boolean useTextAsFilterSeparator;

    public GetClientSettingsResult() {
    }

    public GetClientSettingsResult(long busyDialogTimeout, boolean devMode, String projectLSFDir, boolean showDetailedInfo, boolean forbidDuplicateForms, boolean showNotDefinedStrings,
                                   boolean pivotOnlySelectedColumn, String matchSearchSeparator, GColorTheme colorTheme, Map<String, String> versionedColorThemesCss,
                                   GColorPreferences colorPreferences, String dateFormat, String timeFormat, String staticImagesURL, String[] preDefinedDateRangesNames,
                                   boolean useTextAsFilterSeparator) {
        this.busyDialogTimeout = busyDialogTimeout;
        this.devMode = devMode;
        this.projectLSFDir = projectLSFDir;
        this.showDetailedInfo = showDetailedInfo;
        this.forbidDuplicateForms = forbidDuplicateForms;
        this.showNotDefinedStrings = showNotDefinedStrings;
        this.pivotOnlySelectedColumn = pivotOnlySelectedColumn;
        this.matchSearchSeparator = matchSearchSeparator;
        this.colorTheme = colorTheme;
        this.versionedColorThemesCss = versionedColorThemesCss;
        this.colorPreferences = colorPreferences;
        this.dateFormat = dateFormat;
        this.timeFormat = timeFormat;
        this.staticImagesURL = staticImagesURL;
        this.preDefinedDateRangesNames = preDefinedDateRangesNames;
        this.useTextAsFilterSeparator = useTextAsFilterSeparator;
    }
}