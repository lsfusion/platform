package lsfusion.gwt.client.controller.remote.action.navigator;

import lsfusion.gwt.client.form.object.table.grid.user.design.GColorPreferences;
import lsfusion.gwt.client.view.GColorTheme;

import java.io.Serializable;
import java.util.Map;

public class GClientSettings implements Serializable {
    public long busyDialogTimeout;
    public boolean devMode;
    public String projectLSFDir;
    public boolean showDetailedInfo;
    public boolean autoReconnectOnConnectionLost;
    public int showDetailedInfoDelay;
    public boolean forbidDuplicateForms;
    public boolean pivotOnlySelectedColumn;
    public String matchSearchSeparator;
    public GColorTheme colorTheme;
    public boolean useBootstrap;
    public Map<String, String> versionedColorThemesCss;
    public GColorPreferences colorPreferences;
    public String dateFormat;
    public String timeFormat;
    public String staticImagesURL;
    public String[] preDefinedDateRangesNames;
    public boolean useTextAsFilterSeparator;
    public boolean verticalNavbar;

    public boolean userFiltersManualApplyMode;

    @SuppressWarnings("unused")
    public GClientSettings() {
    }

    public GClientSettings(long busyDialogTimeout, boolean devMode, String projectLSFDir, boolean showDetailedInfo, int showDetailedInfoDelay, boolean autoReconnectOnConnectionLost, boolean forbidDuplicateForms,
                           boolean pivotOnlySelectedColumn, String matchSearchSeparator, GColorTheme colorTheme, boolean useBootstrap, Map<String, String> versionedColorThemesCss,
                           GColorPreferences colorPreferences, String dateFormat, String timeFormat, String staticImagesURL, String[] preDefinedDateRangesNames,
                           boolean useTextAsFilterSeparator, boolean verticalNavbar, boolean userFiltersManualApplyMode) {
        this.busyDialogTimeout = busyDialogTimeout;
        this.devMode = devMode;
        this.projectLSFDir = projectLSFDir;
        this.showDetailedInfo = showDetailedInfo;
        this.autoReconnectOnConnectionLost = autoReconnectOnConnectionLost;
        this.showDetailedInfoDelay = showDetailedInfoDelay;
        this.forbidDuplicateForms = forbidDuplicateForms;
        this.pivotOnlySelectedColumn = pivotOnlySelectedColumn;
        this.matchSearchSeparator = matchSearchSeparator;
        this.colorTheme = colorTheme;
        this.versionedColorThemesCss = versionedColorThemesCss;
        this.useBootstrap = useBootstrap;
        this.colorPreferences = colorPreferences;
        this.dateFormat = dateFormat;
        this.timeFormat = timeFormat;
        this.staticImagesURL = staticImagesURL;
        this.preDefinedDateRangesNames = preDefinedDateRangesNames;
        this.useTextAsFilterSeparator = useTextAsFilterSeparator;
        this.verticalNavbar = verticalNavbar;
        this.userFiltersManualApplyMode = userFiltersManualApplyMode;
    }
}