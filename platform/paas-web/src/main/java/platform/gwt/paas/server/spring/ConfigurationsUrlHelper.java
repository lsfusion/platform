package platform.gwt.paas.server.spring;

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigurationsUrlHelper {
    private static final Pattern CONFIGURATION_URL_PATTERN = Pattern.compile("/configurations/(\\d+)/.*");
    private static final Pattern CONFIGURATION_DYNA_URLS_PATTERN = Pattern. compile("/configurations/\\d+/(dispatch|form\\.jsp|login\\.jsp)");

    public static final String CONFIGURATION_ID_KEY = "PAAS_CONFIGURATION_ID";

    public static int getConfigurationIdFromRequest(HttpServletRequest request) {
        String requestUri = request.getServletPath();
        Matcher m = CONFIGURATION_URL_PATTERN.matcher(requestUri);
        if (m.matches()) {
            return Integer.parseInt(m.group(1));
        }

        return -1;
    }

    public static boolean isConfigurationRequest(HttpServletRequest request) {
        return getConfigurationIdFromRequest(request) != -1;
    }

    public static String getConfigurationRelativeDynamicEnd(HttpServletRequest request) {
        String requestUri = request.getServletPath();
        Matcher m = CONFIGURATION_DYNA_URLS_PATTERN.matcher(requestUri);
        if (m.matches()) {
            return m.group(1);
        }

        return null;
    }

    public static String getLoginUrl(HttpServletRequest request) {
        int configurationId = getConfigurationIdFromRequest(request);
        if (configurationId != -1) {
            return "/configurations/" + configurationId + "/login.jsp";
        } else {
            return "/Paas.jsp#login";
        }
    }

    public static String getSuccessfullLogoutUrl(HttpServletRequest request) {
        int configurationId = getConfigurationIdFromRequest(request);
        if (configurationId != -1) {
            return "/configurations/" + configurationId + "/form.jsp";
        } else {
            return "/Paas.jsp#login";
        }
    }

    public static String getSuccessfullLoginUrl(HttpServletRequest request) {
        int configurationId = getConfigurationIdFromRequest(request);
        if (configurationId != -1) {
            return "/configurations/" + configurationId + "/form.jsp";
        } else {
            return "/Paas.jsp";
        }
    }

    public static String getUnsuccessfullLoginUrl(HttpServletRequest request) {
        int configurationId = getConfigurationIdFromRequest(request);
        if (configurationId != -1) {
            return "/configurations/" + configurationId + "/login.jsp?error=1";
        } else {
            return "/Paas.jsp#login";
        }
    }

    public static String getSuccessfullLoginConstant() {
        // =md5("Login success!!!")
        return "97afd752ecc187ba1dca4aa39d2bbd3a";
    }
}
