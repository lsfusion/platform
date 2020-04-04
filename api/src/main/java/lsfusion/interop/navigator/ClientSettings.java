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
    public boolean showDetailedInfo;
    public boolean forbidDuplicateForms;
    public ColorTheme colorTheme;
    public ColorPreferences colorPreferences;

    public ClientSettings(LocalePreferences localePreferences, String currentUserName, Integer fontSize, boolean busyDialog, 
                          long busyDialogTimeout, boolean useRequestTimeout, boolean devMode, boolean showDetailedInfo,
                          boolean forbidDuplicateForms, ColorTheme colorTheme, ColorPreferences colorPreferences) {
        this.localePreferences = localePreferences;
        this.currentUserName = currentUserName;
        this.fontSize = fontSize;
        this.busyDialog = busyDialog;
        this.busyDialogTimeout = busyDialogTimeout;
        this.useRequestTimeout = useRequestTimeout;
        this.devMode = devMode;
        this.showDetailedInfo = showDetailedInfo;
        this.forbidDuplicateForms = forbidDuplicateForms;
        this.colorTheme = colorTheme;
        this.colorPreferences = colorPreferences;
    }
}