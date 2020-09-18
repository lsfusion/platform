package lsfusion.http.controller;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoadExternalResources {

    private static final String SCRIPT_ONLINE = "<script type=\"text/javascript\" src=\"%s\"></script>";
    private static final String SCRIPT_OFFLINE = "<script type=\"text/javascript\" src=\"static/js/external/%s\"></script>";
    private static final String LINK_ONLINE = "<link rel=\"stylesheet\" type=\"text/css\" href=\"%s\" />";
    private static final String LINK_OFFLINE = "<link rel=\"stylesheet\" type=\"text/css\" href=\"static/css/external/%s\" />";

    public static String getUrl(String url, String alternativeName){
        if (url.endsWith(".js")) {
            return getString(url, alternativeName, SCRIPT_ONLINE, SCRIPT_OFFLINE);
        } else {
            return getString(url, alternativeName, LINK_ONLINE, LINK_OFFLINE);
        }
    }

    public static String getString(String url, String alternativeName, String scriptOnline, String scriptOffline) {
        if (checkURLReachable(url)) {
            return String.format(scriptOnline, url);
        } else {
            if (alternativeName == null) {
                return String.format(scriptOffline, url.split("/")[url.split("/").length - 1]);
            } else {
                return String.format(scriptOffline, alternativeName);
            }
        }
    }

    private static boolean checkURLReachable(String url){
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("HEAD");
            connection.setReadTimeout(1000);
            int responseCode = connection.getResponseCode();
            return responseCode == 200;
        } catch (IOException e) {
            return false;
        }
    }
}
