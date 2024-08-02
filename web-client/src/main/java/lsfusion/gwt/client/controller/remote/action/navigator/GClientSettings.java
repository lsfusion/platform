package lsfusion.gwt.client.controller.remote.action.navigator;

import com.google.gwt.user.client.rpc.IsSerializable;
import lsfusion.gwt.client.form.object.table.grid.user.design.GColorPreferences;
import lsfusion.gwt.client.view.GColorTheme;

import java.io.Serializable;
import java.util.Map;

public class GClientSettings implements Serializable, IsSerializable {
    public long busyDialogTimeout;
    public boolean devMode;
    public String projectLSFDir;
    public boolean showDetailedInfo;
    public boolean autoReconnectOnConnectionLost;
    public int showDetailedInfoDelay;
    public boolean suppressOnFocusChange;
    public boolean forbidDuplicateForms;
    public boolean pivotOnlySelectedColumn;
    public String matchSearchSeparator;
    public GColorTheme colorTheme;
    public boolean useBootstrap;
    public Map<String, String> versionedColorThemesCss;
    public GColorPreferences colorPreferences;
    public String dateFormat;
    public String timeFormat;
    public Integer twoDigitYearStart;
    public String staticImagesURL;
    public String[] preDefinedDateRangesNames;
    public boolean useTextAsFilterSeparator;
    public boolean verticalNavbar;

    public boolean userFiltersManualApplyMode;

    public boolean disableActionsIfReadonly;
    public boolean enableShowingRecentlyLogMessages;
    public String pushNotificationPublicKey;
    
    public double maxStickyLeft;

    public boolean jasperReportsIgnorePageMargins;

    @SuppressWarnings("unused")
    public GClientSettings() {
    }

    public GClientSettings(long busyDialogTimeout, boolean devMode, String projectLSFDir, boolean showDetailedInfo, int showDetailedInfoDelay, boolean suppressOnFocusChange,
                           boolean autoReconnectOnConnectionLost, boolean forbidDuplicateForms, boolean pivotOnlySelectedColumn,
                           String matchSearchSeparator, GColorTheme colorTheme, boolean useBootstrap, Map<String, String> versionedColorThemesCss,
                           GColorPreferences colorPreferences, String dateFormat, String timeFormat, Integer twoDigitYearStart, String staticImagesURL,
                           String[] preDefinedDateRangesNames, boolean useTextAsFilterSeparator, boolean verticalNavbar, boolean userFiltersManualApplyMode,
                           boolean disableActionsIfReadonly, boolean enableShowingRecentlyLogMessages, String pushNotificationPublicKey,
                           double maxStickyLeft, boolean jasperReportsIgnorePageMargins) {
        this.busyDialogTimeout = busyDialogTimeout;
        this.devMode = devMode;
        this.projectLSFDir = projectLSFDir;
        this.showDetailedInfo = showDetailedInfo;
        this.autoReconnectOnConnectionLost = autoReconnectOnConnectionLost;
        this.showDetailedInfoDelay = showDetailedInfoDelay;
        this.suppressOnFocusChange = suppressOnFocusChange;
        this.forbidDuplicateForms = forbidDuplicateForms;
        this.pivotOnlySelectedColumn = pivotOnlySelectedColumn;
        this.matchSearchSeparator = matchSearchSeparator;
        this.colorTheme = colorTheme;
        this.versionedColorThemesCss = versionedColorThemesCss;
        this.useBootstrap = useBootstrap;
        this.colorPreferences = colorPreferences;
        this.dateFormat = dateFormat;
        this.timeFormat = timeFormat;
        this.twoDigitYearStart = twoDigitYearStart;
        this.staticImagesURL = staticImagesURL;
        this.preDefinedDateRangesNames = preDefinedDateRangesNames;
        this.useTextAsFilterSeparator = useTextAsFilterSeparator;
        this.verticalNavbar = verticalNavbar;
        this.userFiltersManualApplyMode = userFiltersManualApplyMode;
        this.disableActionsIfReadonly = disableActionsIfReadonly;
        this.enableShowingRecentlyLogMessages = enableShowingRecentlyLogMessages;
        this.pushNotificationPublicKey = pushNotificationPublicKey;
        this.maxStickyLeft = maxStickyLeft;
        this.jasperReportsIgnorePageMargins = jasperReportsIgnorePageMargins;
    }
}