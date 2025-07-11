package lsfusion.interop.logics;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.base.view.ColorTheme;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.LocalePreferences;
import lsfusion.interop.form.object.table.grid.user.design.ColorPreferences;
import lsfusion.interop.logics.remote.RemoteLogicsInterface;
import lsfusion.interop.navigator.ClientSettings;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;
import lsfusion.interop.session.*;
import org.apache.commons.net.util.Base64;
import org.castor.core.util.Base64Decoder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.rmi.RemoteException;
import java.util.List;
import java.util.*;

import static lsfusion.base.BaseUtils.trimToNull;

public class LogicsSessionObject {

    public final RemoteLogicsInterface remoteLogics;

    public final LogicsConnection connection;

    public LogicsSessionObject(RemoteLogicsInterface remoteLogics, LogicsConnection connection) {
        this.remoteLogics = remoteLogics;
        this.connection = connection;
    }

    public ServerSettings serverSettings; // caching
    public static ClientSettings getClientSettings(ExternalRequest externalRequest, RemoteNavigatorInterface remoteNavigator, ConvertFileValue convertFileValue) throws RemoteException {
        ExternalResponse result = remoteNavigator.exec("Service.getClientSettings[]", externalRequest);
        JSONObject json = new JSONObject(getStringResult(result, convertFileValue));

        String currentUserName = json.optString("currentUserName");
        Integer fontSize = !json.has("fontSize") ? null : json.optInt("fontSize");
        boolean useBusyDialog = json.optBoolean("useBusyDialog");
        boolean useRequestTimeout = json.optBoolean("useRequestTimeout");
        boolean forbidDuplicateForms = json.optBoolean("forbidDuplicateForms");
        boolean autoReconnectOnConnectionLost = json.optBoolean("autoReconnectOnConnectionLost");
        boolean showDetailedInfo = json.optBoolean("showDetailedInfo");
        int showDetailedInfoDelay = json.optInt("showDetailedInfoDelay");
        boolean mobile = json.optBoolean("mobile");
        boolean suppressOnFocusChange = json.optBoolean("suppressOnFocusChange");
        boolean devMode = json.optBoolean("devMode");
        String projectLSFDir = json.optString("projectLSFDir");

        ColorTheme colorTheme = BaseUtils.nvl(ColorTheme.get(json.optString("colorThemeString")), ColorTheme.DEFAULT);
        boolean useBootstrap = json.optBoolean("useBootstrap");

        String size = json.optString("size");

        boolean verticalNavbar = json.optBoolean("verticalNavbar");

        String selectedRowBackground = json.optString("selectedRowBackground");
        String selectedCellBackground = json.optString("selectedCellBackground");
        String focusedCellBackground = json.optString("focusedCellBackground");
        String focusedCellBorder = json.optString("focusedCellBorder");
        String tableGridColor = json.optString("tableGridColor");

        ColorPreferences colorPreferences = new ColorPreferences(selectedRowBackground.isEmpty() ? null : Color.decode(selectedRowBackground),
                selectedCellBackground.isEmpty() ? null : Color.decode(selectedCellBackground),
                focusedCellBackground.isEmpty() ? null : Color.decode(focusedCellBackground),
                focusedCellBorder.isEmpty() ? null : Color.decode(focusedCellBorder),
                tableGridColor.isEmpty() ? null : Color.decode(tableGridColor));

        List<String> preDefinedDateRangesNames = new ArrayList<>();
        fillRanges(json.optJSONArray("dateTimePickerRanges"), preDefinedDateRangesNames);
        fillRanges(json.optJSONArray("intervalPickerRanges"), preDefinedDateRangesNames);

        String language = json.optString("language");
        String country = json.optString("country");
        Locale locale = LocalePreferences.getLocale(language, country);

        String timeZone = json.optString("timeZone");
        Integer twoDigitYearStart = !json.has("twoDigitYearStart") ? null : json.optInt("twoDigitYearStart");
        String dateFormat = json.optString("dateFormat");
        String timeFormat = json.optString("timeFormat");

        LocalePreferences localePreferences = new LocalePreferences(locale, timeZone, twoDigitYearStart, dateFormat, timeFormat);

        long busyDialogTimeout = json.optLong("busyDialogTimeout");
        boolean showNotDefinedStrings = json.optBoolean("showNotDefinedStrings");
        boolean pivotOnlySelectedColumn = json.optBoolean("pivotOnlySelectedColumn");
        String matchSearchSeparator = json.optString("matchSearchSeparator");
        boolean useTextAsFilterSeparator = json.optBoolean("useTextAsFilterSeparator");
        boolean userFiltersManualApplyMode = json.optBoolean("userFiltersManualApplyMode");
        boolean disableActionsIfReadonly = json.optBoolean("disableActionsIfReadonly");
        boolean enableShowingRecentlyLogMessages = json.optBoolean("enableShowingRecentlyLogMessages");
        String pushNotificationPublicKey = json.optString("pushNotificationPublicKey");
        int maxRequestQueueSize = json.optInt("maxRequestQueueSize");
        double maxStickyLeft = json.optDouble("maxStickyLeft");
        boolean jasperReportsIgnorePageMargins = json.optBoolean("jasperReportsIgnorePageMargins");
        double cssBackwardCompatibilityLevel = json.optDouble("cssBackwardCompatibilityLevel");
        boolean useClusterizeInPivot = json.optBoolean("useClusterizeInPivot");
        String computerSettings = json.optString("computerSettings");

        return new ClientSettings(localePreferences, currentUserName, fontSize, useBusyDialog, busyDialogTimeout, useRequestTimeout, devMode,
                projectLSFDir, showDetailedInfo, showDetailedInfoDelay, mobile, suppressOnFocusChange, autoReconnectOnConnectionLost, forbidDuplicateForms, showNotDefinedStrings,
                pivotOnlySelectedColumn, matchSearchSeparator,
                colorTheme, useBootstrap, size, colorPreferences, preDefinedDateRangesNames.toArray(new String[0]), useTextAsFilterSeparator,
                verticalNavbar, userFiltersManualApplyMode, disableActionsIfReadonly,
                enableShowingRecentlyLogMessages, pushNotificationPublicKey, maxRequestQueueSize, maxStickyLeft, jasperReportsIgnorePageMargins,
                cssBackwardCompatibilityLevel, useClusterizeInPivot, computerSettings);
    }

    // Expect that only JSONObject and JSONArray will be passed as param
    private static Map<String, String> getMapFromJSON(Object json) {
        // If JSON contains only one object, it is JSONObject, if multiple objects - JSONArray.
        // If we call the optJSONArray() on JSONObject, we get null, same with optJSONObject.
        if (json instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) json;
            return Collections.singletonMap(jsonObject.optString("key"), jsonObject.optString("value", null));
        }

        JSONArray jsonArray = (JSONArray) json;
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.optJSONObject(i);
            if (jsonObject != null)
                map.put(jsonObject.optString("key"), jsonObject.optString("value", null));
        }
        return map;
    }

    private static List<Pair<String, RawFileData>> getFileData(JSONObject json, String field) {
        if (json.has(field)) {
            List<Pair<String, RawFileData>> resultFiles = new LinkedList<>();
            getMapFromJSON(json.opt(field)).forEach((fileName, file) -> resultFiles.add(Pair.create(fileName, file != null ? new RawFileData(Base64.decodeBase64(file)) : null)));
            return resultFiles;
        }
        return null;
    }

    private FileData getFileData(String base64) {
        return base64 != null ? new FileData(Base64Decoder.decode(base64)) : null;
    }

    public static InitSettings getInitSettings(SessionInfo sessionInfo, RemoteNavigatorInterface remoteNavigator, ConvertFileValue convertFileValue) throws RemoteException {
        ExternalResponse result = remoteNavigator.exec("Service.getInitSettings[]", sessionInfo.externalRequest);
        JSONObject json = new JSONObject(getStringResult(result, convertFileValue));

        List<Pair<String, RawFileData>> mainResourcesBeforeSystem = getFileData(json,"mainResourcesBeforeSystem");
        List<Pair<String, RawFileData>> mainResourcesAfterSystem = getFileData(json,"mainResourcesAfterSystem");

        return new InitSettings(mainResourcesBeforeSystem, mainResourcesAfterSystem);
    }

    public static class InitSettings {

        public List<Pair<String, RawFileData>> mainResourcesBeforeSystem;
        public List<Pair<String, RawFileData>> mainResourcesAfterSystem;

        public InitSettings(List<Pair<String, RawFileData>> mainResourcesBeforeSystem, List<Pair<String, RawFileData>> mainResourcesAfterSystem) {
            this.mainResourcesBeforeSystem = mainResourcesBeforeSystem;
            this.mainResourcesAfterSystem = mainResourcesAfterSystem;
        }
    }

    public static String getStringResult(ExternalResponse result, ConvertFileValue convertFileValue) {
        ExternalRequest.Result exResult = ((ResultExternalResponse) result).results[0];
        if(convertFileValue != null) // in the desktop client it can be null
            exResult = exResult.convertFileValue(convertFileValue);
        return ((FileData) exResult.value).getRawFile().getString(ExternalUtils.defaultBodyCharset); // because we don't send any charset and thus defaultBodyCharset will be used in the result
    }

    private static void fillRanges(JSONArray rangesJson, List<String> ranges) {
        if(rangesJson != null)
            for (int i = 0; i < rangesJson.length(); i++) {
                ranges.add(rangesJson.getJSONObject(i).getString("range"));
           }
    }

    public ServerSettings getServerSettings(SessionInfo sessionInfo, String contextPath, boolean noCache, ConvertFileValue convertFileValue) throws RemoteException {
        if(serverSettings == null || serverSettings.inDevMode || noCache) {
            ExternalResponse result = remoteLogics.exec(AuthenticationToken.ANONYMOUS, sessionInfo.connectionInfo, "Service.getServerSettings[]", sessionInfo.externalRequest);
            JSONObject json = new JSONObject(getStringResult(result, convertFileValue));

            String logicsName = trimToNull(json.optString("logicsName"));
            String displayName = trimToNull(json.optString("displayName"));
            FileData logicsLogo = getFileData(trimToNull(json.optString("logicsLogo")));
            FileData logicsIcon = getFileData(trimToNull(json.optString("logicsIcon")));
            FileData PWAIcon = getFileData(trimToNull(json.optString("PWAIcon")));
            String platformVersion = trimToNull(json.optString("platformVersion"));
            Integer apiVersion = json.optInt("apiVersion");
            boolean inDevMode = json.optBoolean("inDevMode");
            int sessionConfigTimeout = json.optInt("sessionConfigTimeout");
            boolean anonymousUI = json.optBoolean("anonymousUI");
            String jnlpUrls = trimToNull(json.optString("jnlpUrls"));

            boolean disableRegistration = json.optBoolean("disableRegistration");
            Map<String, String> lsfParams = json.has("lsfParams") ? getMapFromJSON(json.opt("lsfParams")) : null;

            List<Pair<String, RawFileData>> noAuthResourcesBeforeSystem = getFileData(json, "noAuthResourcesBeforeSystem");
            List<Pair<String, RawFileData>> noAuthResourcesAfterSystem = getFileData(json, "noAuthResourcesAfterSystem");

            serverSettings = new ServerSettings(logicsName, displayName, logicsLogo, logicsIcon, PWAIcon, platformVersion, apiVersion, inDevMode,
                    sessionConfigTimeout, anonymousUI, jnlpUrls, disableRegistration, lsfParams, noAuthResourcesBeforeSystem, noAuthResourcesAfterSystem);
        }
        return serverSettings;
    }
}
