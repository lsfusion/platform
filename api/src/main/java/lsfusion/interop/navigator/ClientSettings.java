package lsfusion.interop.navigator;

import lsfusion.interop.base.view.ColorTheme;
import lsfusion.interop.connection.LocalePreferences;
import lsfusion.interop.form.object.table.grid.user.design.ColorPreferences;

import java.io.Serializable;

public class ClientSettings implements Serializable {
    public LocalePreferences localePreferences;
    public String currentUserName;
    public Integer fontSize;
    public boolean busyDialog;
    public long busyDialogTimeout;
    public boolean useRequestTimeout;
    public boolean devMode;
    public String projectLSFDir;
    public boolean showDetailedInfo;
    public boolean autoReconnectOnConnectionLost;
    public int showDetailedInfoDelay;
    public Boolean mobileMode;
    public boolean suppressOnFocusChange;
    public boolean contentWordWrap;
    public boolean forbidDuplicateForms;
    public boolean showNotDefinedStrings;
    public boolean pivotOnlySelectedColumn;
    public String matchSearchSeparator;
    public ColorTheme colorTheme;
    public boolean useBootstrap;
    public String size;
    public ColorPreferences colorPreferences;
    public String[] preDefinedDateRangesNames;
    public boolean useTextAsFilterSeparator;

    public boolean verticalNavbar;
    public boolean userFiltersManualApplyMode;
    public boolean disableActionsIfReadonly;
    public boolean enableShowingRecentlyLogMessages;
    public String pushNotificationPublicKey;
    public int maxRequestQueueSize;
    public double maxStickyLeft;
    public boolean jasperReportsIgnorePageMargins;
    public double cssBackwardCompatibilityLevel;


    public ClientSettings(LocalePreferences localePreferences, String currentUserName, Integer fontSize, boolean busyDialog, long busyDialogTimeout,
                          boolean useRequestTimeout, boolean devMode, String projectLSFDir, boolean showDetailedInfo, int showDetailedInfoDelay,
                          Boolean mobileMode, boolean suppressOnFocusChange, boolean autoReconnectOnConnectionLost, boolean forbidDuplicateForms,
                          boolean showNotDefinedStrings, boolean pivotOnlySelectedColumn, String matchSearchSeparator, ColorTheme colorTheme,
                          boolean useBootstrap, String size, ColorPreferences colorPreferences, String[] preDefinedDateRangesNames, boolean useTextAsFilterSeparator,
                          boolean verticalNavbar, boolean userFiltersManualApplyMode, boolean disableActionsIfReadonly, boolean enableShowingRecentlyLogMessages,
                          String pushNotificationPublicKey, int maxRequestQueueSize, double maxStickyLeft, boolean jasperReportsIgnorePageMargins,
                          double cssBackwardCompatibilityLevel) {
        this.localePreferences = localePreferences;
        this.currentUserName = currentUserName;
        this.fontSize = fontSize;
        this.busyDialog = busyDialog;
        this.busyDialogTimeout = busyDialogTimeout;
        this.useRequestTimeout = useRequestTimeout;
        this.devMode = devMode;
        this.projectLSFDir = projectLSFDir;
        this.showDetailedInfo = showDetailedInfo;
        this.autoReconnectOnConnectionLost = autoReconnectOnConnectionLost;
        this.showDetailedInfoDelay = showDetailedInfoDelay;
        this.mobileMode = mobileMode;
        this.suppressOnFocusChange = suppressOnFocusChange;
        this.forbidDuplicateForms = forbidDuplicateForms;
        this.showNotDefinedStrings = showNotDefinedStrings;
        this.pivotOnlySelectedColumn = pivotOnlySelectedColumn;
        this.matchSearchSeparator = matchSearchSeparator;
        this.colorTheme = colorTheme;
        this.useBootstrap = useBootstrap;
        this.size = size;
        this.colorPreferences = colorPreferences;
        this.preDefinedDateRangesNames = preDefinedDateRangesNames;
        this.useTextAsFilterSeparator = useTextAsFilterSeparator;
        this.verticalNavbar = verticalNavbar;
        this.userFiltersManualApplyMode = userFiltersManualApplyMode;
        this.disableActionsIfReadonly = disableActionsIfReadonly;
        this.enableShowingRecentlyLogMessages = enableShowingRecentlyLogMessages;
        this.pushNotificationPublicKey = pushNotificationPublicKey;
        this.maxRequestQueueSize = maxRequestQueueSize;
        this.maxStickyLeft = maxStickyLeft;
        this.jasperReportsIgnorePageMargins = jasperReportsIgnorePageMargins;
        this.cssBackwardCompatibilityLevel = cssBackwardCompatibilityLevel;
    }
}