package lsfusion.interop.navigator;

import lsfusion.base.Pair;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.base.view.ColorTheme;
import lsfusion.interop.connection.LocalePreferences;
import lsfusion.interop.form.object.table.grid.user.design.ColorPreferences;

import java.io.Serializable;
import java.util.List;

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
    public boolean suppressOnFocusChange;
    public boolean contentWordWrap;
    public boolean forbidDuplicateForms;
    public boolean showNotDefinedStrings;
    public boolean pivotOnlySelectedColumn;
    public String matchSearchSeparator;
    public ColorTheme colorTheme;
    public boolean useBootstrap;
    public ColorPreferences colorPreferences;
    public String[] preDefinedDateRangesNames;
    public boolean useTextAsFilterSeparator;
    public List<Pair<String, RawFileData>> mainResourcesBeforeSystem;
    public List<Pair<String, RawFileData>> mainResourcesAfterSystem;

    public boolean verticalNavbar;
    public boolean userFiltersManualApplyMode;
    public int maxRequestQueueSize;


    public ClientSettings(LocalePreferences localePreferences, String currentUserName, Integer fontSize, boolean busyDialog, long busyDialogTimeout,
                          boolean useRequestTimeout, boolean devMode, String projectLSFDir, boolean showDetailedInfo, int showDetailedInfoDelay,
                          boolean suppressOnFocusChange, boolean contentWordWrap, boolean autoReconnectOnConnectionLost, boolean forbidDuplicateForms,
                          boolean showNotDefinedStrings, boolean pivotOnlySelectedColumn, String matchSearchSeparator, ColorTheme colorTheme,
                          boolean useBootstrap, ColorPreferences colorPreferences, String[] preDefinedDateRangesNames, boolean useTextAsFilterSeparator,
                          List<Pair<String, RawFileData>> mainResourcesBeforeSystem, List<Pair<String, RawFileData>> mainResourcesAfterSystem,
                          boolean verticalNavbar, boolean userFiltersManualApplyMode, int maxRequestQueueSize) {
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
        this.suppressOnFocusChange = suppressOnFocusChange;
        this.contentWordWrap = contentWordWrap;
        this.forbidDuplicateForms = forbidDuplicateForms;
        this.showNotDefinedStrings = showNotDefinedStrings;
        this.pivotOnlySelectedColumn = pivotOnlySelectedColumn;
        this.matchSearchSeparator = matchSearchSeparator;
        this.colorTheme = colorTheme;
        this.useBootstrap = useBootstrap;
        this.colorPreferences = colorPreferences;
        this.preDefinedDateRangesNames = preDefinedDateRangesNames;
        this.useTextAsFilterSeparator = useTextAsFilterSeparator;
        this.mainResourcesBeforeSystem = mainResourcesBeforeSystem;
        this.mainResourcesAfterSystem = mainResourcesAfterSystem;
        this.verticalNavbar = verticalNavbar;
        this.userFiltersManualApplyMode = userFiltersManualApplyMode;
        this.maxRequestQueueSize = maxRequestQueueSize;
    }
}