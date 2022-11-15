package lsfusion.gwt.client.controller.remote.action.navigator;

import lsfusion.gwt.client.form.object.table.grid.user.design.GColorPreferences;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;
import lsfusion.gwt.client.view.GColorTheme;
import net.customware.gwt.dispatch.shared.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GetSettingsResult implements Result {
//    GetNavigatorInfoResult
    public GNavigatorElement root;
    public ArrayList<GNavigatorWindow> navigatorWindows;
    public GAbstractWindow log;
    public GAbstractWindow status;
    public GAbstractWindow forms;

//    GetClientSettingsResult
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


    public GetSettingsResult() {
    }
    public GetSettingsResult(GNavigatorElement root, ArrayList<GNavigatorWindow> navigatorWindows, List<GAbstractWindow> commonWindows, long busyDialogTimeout, boolean devMode,
                             String projectLSFDir, boolean showDetailedInfo, boolean forbidDuplicateForms, boolean showNotDefinedStrings,
                             boolean pivotOnlySelectedColumn, String matchSearchSeparator, GColorTheme colorTheme,
                             Map<String, String> versionedColorThemesCss, GColorPreferences colorPreferences, String dateFormat,
                             String timeFormat, String staticImagesURL, String[] preDefinedDateRangesNames, boolean useTextAsFilterSeparator) {
        this.root = root;
        this.navigatorWindows = navigatorWindows;
        this.log = commonWindows.get(0);
        this.status = commonWindows.get(1);
        this.forms = commonWindows.get(2);
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