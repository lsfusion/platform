package platform.gwt.base.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.base.client.ui.ErrorFrameWidget;

public class BaseFrame implements EntryPoint {
    public static BaseMessages baseMessages = BaseMessages.Instance.get();

    public abstract class ErrorAsyncCallback<T> implements AsyncCallback<T> {
        @Override
        public void onFailure(Throwable caught) {
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
    }

    public static String getPageUrlPreservingParameters(String pageUrl) {
        return getPageUrlPreservingParameters(pageUrl, null, null);
    }

    public static String getPageUrlPreservingParameters(String param, String value) {
        return getPageUrlPreservingParameters(null, param, value);
    }

    public static String getPageUrlPreservingParameters(String pageUrl, String param, String value) {
        String url;
        if (param != null) {
            url = value != null
                  ? Window.Location.createUrlBuilder().setParameter(param, value).buildString()
                  : Window.Location.createUrlBuilder().removeParameter(param).buildString();
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

    public static String getLogoffUrl() {
        return getPageUrlPreservingParameters("logoff.jsp");
    }

    public static void logoff() {
        com.google.gwt.user.client.Window.open(BaseFrame.getLogoffUrl(), "_self", null);
    }
}
