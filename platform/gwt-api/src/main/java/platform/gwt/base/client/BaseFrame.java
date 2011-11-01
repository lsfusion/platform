package platform.gwt.base.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.http.client.UrlBuilder;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.base.client.ui.ErrorFrameWidget;
import platform.gwt.utils.GwtUtils;

public class BaseFrame implements EntryPoint {
    public static BaseMessages baseMessages = BaseMessages.Instance.get();

    public abstract class ErrorAsyncCallback<T> extends AsyncCallbackEx<T> {
        @Override
        public void failure(Throwable caught) {
            showErrorPage(caught);
        }
    }

    public class UpdateAsyncCallback<T> implements AsyncCallback<T> {
        @Override
        public void onFailure(Throwable caught) {
            showErrorPage(caught);
        }

        @Override
        public void onSuccess(T result) {
            update();
        }
    }

    protected void update() {
    }

    @Override
    public void onModuleLoad() {
    }

    protected static void setAsRootPane(Widget widget) {
        RootPanel.get().clear();
        RootPanel.get().add(widget);
    }

    public static void showErrorPage(Throwable caught) {
        setAsRootPane(new ErrorFrameWidget(caught));
        GwtUtils.removeLoaderFromHostedPage();
    }

    public static String getPageUrlPreservingParameters(String pageUrl) {
        return getPageUrlPreservingParameters(pageUrl, (String[])null, null);
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

    public static String getLogoutUrl() {
        return getPageUrlPreservingParameters("logout.jsp", "spring-security-redirect", URL.encodePathSegment(Window.Location.createUrlBuilder().buildString()));
    }

    public static void logout() {
        com.google.gwt.user.client.Window.open(BaseFrame.getLogoutUrl(), "_self", null);
    }
}
