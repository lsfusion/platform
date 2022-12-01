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
import lsfusion.interop.session.ExternalResponse;
import lsfusion.interop.session.SessionInfo;
import org.apache.commons.net.util.Base64;
import org.castor.core.util.Base64Decoder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static lsfusion.base.BaseUtils.trimToNull;

public class LogicsSessionObject {

    public final RemoteLogicsInterface remoteLogics;

    public final LogicsConnection connection;

    public LogicsSessionObject(RemoteLogicsInterface remoteLogics, LogicsConnection connection) {
        this.remoteLogics = remoteLogics;
        this.connection = connection;
    }

    public ServerSettings serverSettings; // caching
    public ServerSettings getServerSettings(SessionInfo sessionInfo, String contextPath, boolean noCache) throws RemoteException {
        if(serverSettings == null || serverSettings.inDevMode || noCache) {
            JSONObject json = getJSONResult(remoteLogics.exec(AuthenticationToken.ANONYMOUS, sessionInfo, "Service.getServerSettings[]", sessionInfo.externalRequest));

            String logicsName = trimToNull(json.optString("logicsName"));
            String displayName = trimToNull(json.optString("displayName"));
            RawFileData logicsLogo = getRawFileData(trimToNull(json.optString("logicsLogo")));
            RawFileData logicsIcon = getRawFileData(trimToNull(json.optString("logicsIcon")));
            String platformVersion = trimToNull(json.optString("platformVersion"));
            Integer apiVersion = json.optInt("apiVersion");
            boolean inDevMode = json.optBoolean("inDevMode");
            int sessionConfigTimeout = json.optInt("sessionConfigTimeout");
            boolean anonymousUI = json.optBoolean("anonymousUI");
            String jnlpUrls = trimToNull(json.optString("jnlpUrls"));
            if (jnlpUrls != null && contextPath != null)
                jnlpUrls = jnlpUrls.replaceAll("\\{contextPath}", contextPath);

            boolean disableRegistration = json.optBoolean("disableRegistration");
            Map<String, String> lsfParams = getMapFromJSONArray(json.optJSONArray("lsfParams"));
            List<Pair<String, RawFileData>> loginResources = getFileData(json.optJSONArray("loginResources"));

            serverSettings = new ServerSettings(logicsName, displayName, logicsLogo, logicsIcon, platformVersion, apiVersion, inDevMode,
                    sessionConfigTimeout, anonymousUI, jnlpUrls, disableRegistration, lsfParams, loginResources);
        }
        return serverSettings;
    }

    private static Map<String, String> getMapFromJSONArray(JSONArray jsonArray) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.optJSONObject(i);
            map.put(jsonObject.optString("key"), jsonObject.optString("value"));
        }
        return map;
    }

    public static List<Pair<String, RawFileData>> getFileData(JSONArray jsonArray) {
        Map<String, String> files = getMapFromJSONArray(jsonArray);
        List<Pair<String, RawFileData>> resultFiles = new LinkedList<>();
        files.forEach((fileName, file) -> resultFiles.add(Pair.create(fileName, new RawFileData(new String(Base64.decodeBase64(file)).getBytes()))));
        return resultFiles;
    }

    private RawFileData getRawFileData(String base64) {
        return base64 != null ? new RawFileData(Base64Decoder.decode(base64)) : null;
    }

    public static ClientSettings getClientSettings(SessionInfo sessionInfo, RemoteNavigatorInterface remoteNavigator) throws RemoteException {
        JSONObject json = getJSONResult(remoteNavigator.exec("Service.getClientSettings[]", sessionInfo.externalRequest));

        String currentUserName = json.optString("currentUserName");
        Integer fontSize = !json.has("fontSize") ? null : json.optInt("fontSize");
        boolean useBusyDialog = json.optBoolean("useBusyDialog");
        boolean useRequestTimeout = json.optBoolean("useRequestTimeout");
        boolean forbidDuplicateForms = json.optBoolean("forbidDuplicateForms");
        boolean showDetailedInfo = json.optBoolean("showDetailedInfo");
        boolean devMode = json.optBoolean("devMode");
        String projectLSFDir = json.optString("projectLSFDir");

        ColorTheme colorTheme = BaseUtils.nvl(ColorTheme.get(json.optString("colorThemeString")), ColorTheme.DEFAULT);
        boolean useBootstrap = json.optBoolean("useBootstrap");

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

        List<Pair<String, RawFileData>> mainResourcesData = getFileData(json.getJSONArray("mainResources"));

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

        return new ClientSettings(localePreferences, currentUserName, fontSize, useBusyDialog, busyDialogTimeout, useRequestTimeout, devMode,
                projectLSFDir, showDetailedInfo, forbidDuplicateForms, showNotDefinedStrings, pivotOnlySelectedColumn, matchSearchSeparator,
                colorTheme, useBootstrap, colorPreferences, preDefinedDateRangesNames.toArray(new String[0]), useTextAsFilterSeparator, mainResourcesData);
    }

    private static void fillRanges(JSONArray rangesJson, List<String> ranges) {
        for (int i = 0; i < rangesJson.length(); i++) {
            ranges.add(rangesJson.getJSONObject(i).getString("range"));
        }
    }

    private static JSONObject getJSONResult(ExternalResponse result) {
        return new JSONObject(new String(((FileData) result.results[0]).getRawFile().getBytes(), StandardCharsets.UTF_8));
    }
}
