package lsfusion.interop.navigator;

import lsfusion.interop.base.view.ColorTheme;
import lsfusion.interop.connection.LocalePreferences;

import java.io.Serializable;

public class ClientSettings implements Serializable {
    public LocalePreferences localePreferences;
    public String currentUserName;
    public Integer fontSize;
    public boolean busyDialog;
    public long busyDialogTimeout;
    public boolean useRequestTimeout;
    public boolean devMode;
    public boolean configurationAccessAllowed;
    public boolean forbidDuplicateForms;
    public ColorTheme colorTheme;

    public ClientSettings(LocalePreferences localePreferences, String currentUserName, Integer fontSize, boolean busyDialog, long busyDialogTimeout, boolean useRequestTimeout,
                          boolean devMode, boolean configurationAccessAllowed, boolean forbidDuplicateForms, ColorTheme colorTheme) {
        this.localePreferences = localePreferences;
        this.currentUserName = currentUserName;
        this.fontSize = fontSize;
        this.busyDialog = busyDialog;
        this.busyDialogTimeout = busyDialogTimeout;
        this.useRequestTimeout = useRequestTimeout;
        this.devMode = devMode;
        this.configurationAccessAllowed = configurationAccessAllowed;
        this.forbidDuplicateForms = forbidDuplicateForms;
        this.colorTheme = colorTheme;
    }
}