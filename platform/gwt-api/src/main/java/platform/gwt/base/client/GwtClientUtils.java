package platform.gwt.base.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.http.client.UrlBuilder;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

public class GwtClientUtils {
    public static BaseMessages baseMessages = BaseMessages.Instance.get();
    public static final String TARGET_PARAM = "targetUrl";

    public static void removeLoaderFromHostedPage() {
        RootPanel p = RootPanel.get("loadingWrapper");
        if (p != null) {
            RootPanel.getBodyElement().removeChild(p.getElement());
        }
    }

    public static void setAsRootPane(Widget widget) {
        RootPanel.get().clear();
        RootPanel.get().add(widget);
    }

    public static String getPageUrlPreservingParameters(String pageUrl) {
        return getPageUrlPreservingParameters(pageUrl, (String[]) null, null);
    }

    public static String getPageUrlPreservingParameters(String param, String value) {
        return getPageUrlPreservingParameters(null, param, value);
    }

    public static String getPageUrlPreservingParameters(String pageUrl, String param, String value) {
        return getPageUrlPreservingParameters(pageUrl, param, value, null, null);
    }

    public static String getPageUrlPreservingParameters(String pageUrl, String param1, String value1, String param2, String value2) {
        return getPageUrlPreservingParameters(pageUrl, new String[]{param1, param2}, new String[]{value1, value2});
    }

    public static String getPageUrlPreservingParameters(String pageUrl, String[] params, String[] values) {
        String url;
        if (params != null && params.length > 0) {
            UrlBuilder urlBuilder = Window.Location.createUrlBuilder();
            for (int i = 0; i < params.length; ++i) {
                String param = params[i];
                String value = values[i];

                if (value != null) {
                    urlBuilder.setParameter(param, value).buildString();
                } else {
                    urlBuilder.removeParameter(param).buildString();
                }
            }
            url = urlBuilder.buildString();
        } else {
            url = Window.Location.getQueryString();
        }

        //использовать текущую страницу
        if (pageUrl == null) {
            return url;
        }

        int paramBegin = url.indexOf("?");
        if (paramBegin == -1) {
            paramBegin = url.length();
        }

        return GWT.getHostPageBaseURL() + pageUrl + url.substring(paramBegin);
    }

    public static String getCurrentUrlEncoded() {
        return URL.encodePathSegment(Window.Location.createUrlBuilder().buildString());
    }

    public static String getLogoutUrl() {
        return getPageUrlPreservingParameters("logout.jsp", TARGET_PARAM, getCurrentUrlEncoded());
    }

    public static String getLoginUrl() {
        return getPageUrlPreservingParameters("login.jsp", TARGET_PARAM, getCurrentUrlEncoded());
    }

    public static void relogin() {
        Window.open(GwtClientUtils.getLoginUrl(), "_self", null);
    }

    public static void logout() {
        Window.open(GwtClientUtils.getLogoutUrl(), "_self", null);
    }

    public static String toHtml(String plainString) {
        if (plainString == null) {
            return "";
        }
        return SimpleHtmlSanitizer.sanitizeHtml(plainString).asString().replaceAll("(\r\n|\n\r|\r|\n)", "<br />");
    }
}
