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
    public boolean forbidDuplicateForms;
    public boolean showNotDefinedStrings;
    public boolean pivotOnlySelectedColumn;
    public String matchSearchSeparator;
    public ColorTheme colorTheme;
    public ColorPreferences colorPreferences;
    public String[] preDefinedDateRangesNames;
    public boolean useTextAsFilterSeparator;

    public ClientSettings(LocalePreferences localePreferences, String currentUserName, Integer fontSize, boolean busyDialog,
                          long busyDialogTimeout, boolean useRequestTimeout, boolean devMode, String projectLSFDir,
                          boolean showDetailedInfo, boolean forbidDuplicateForms, boolean showNotDefinedStrings,
                          boolean pivotOnlySelectedColumn, String matchSearchSeparator,
                          ColorTheme colorTheme, ColorPreferences colorPreferences, String[] preDefinedDateRangesNames,
                          boolean useTextAsFilterSeparator) {
        this.localePreferences = localePreferences;
        this.currentUserName = currentUserName;
        this.fontSize = fontSize;
        this.busyDialog = busyDialog;
        this.busyDialogTimeout = busyDialogTimeout;
        this.useRequestTimeout = useRequestTimeout;
        this.devMode = devMode;
        this.projectLSFDir = projectLSFDir;
        this.showDetailedInfo = showDetailedInfo;
        this.forbidDuplicateForms = forbidDuplicateForms;
        this.showNotDefinedStrings = showNotDefinedStrings;
        this.pivotOnlySelectedColumn = pivotOnlySelectedColumn;
        this.matchSearchSeparator = matchSearchSeparator;
        this.colorTheme = colorTheme;
        this.colorPreferences = colorPreferences;
        this.preDefinedDateRangesNames = preDefinedDateRangesNames;
        this.useTextAsFilterSeparator = useTextAsFilterSeparator;
    }
}