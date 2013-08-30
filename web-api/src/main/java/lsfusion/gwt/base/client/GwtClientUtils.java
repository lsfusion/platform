package lsfusion.gwt.base.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.http.client.URL;
import com.google.gwt.http.client.UrlBuilder;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.HasPreferredSize;

import java.util.*;

import static java.lang.Math.max;

public class GwtClientUtils {
    public static final String TIMEOUT_MESSAGE = "SESSION_TIMED_OUT";
    public static final String TARGET_URL_PARAM = "targetUrl";
    public static final String GWT_DEVMODE_PARAM = "gwt.codesvr";

    public static final BaseMessages baseMessages = BaseMessages.Instance.get();

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
        return getPageUrlPreservingParameters("logout", TARGET_URL_PARAM, getCurrentUrlEncoded());
    }

    public static String getLoginUrl() {
        return getPageUrlPreservingParameters("login.jsp", TARGET_URL_PARAM, getCurrentUrlEncoded());
    }

    public static void relogin() {
        Window.open(GwtClientUtils.getLoginUrl(), "_self", null);
    }

    public static void logout() {
        Window.open(GwtClientUtils.getLogoutUrl(), "_self", null);
    }

    public static Map<String, String> getPageParameters() {
        Map<String, String> params = new HashMap<String, String>();
        try {
            Dictionary dict = Dictionary.getDictionary("parameters");
            if (dict != null) {
                for (String param : dict.keySet()) {
                    params.put(param, dict.get(param));
                }
                return params;
            }
        } catch (Exception ignored) {
        }

        try {
            Map<String, List<String>> paramMap = Window.Location.getParameterMap();
            for (String param : paramMap.keySet()) {
                params.put(param, paramMap.get(param).isEmpty() ? null : paramMap.get(param).get(0));
            }
        } catch (Exception ignored) {
        }

        return params;
    }

    public static String getPageParameter(String parameterName) {
        return getPageParameters().get(parameterName);
    }

    public static int getIntPageParameter(String parameterName) {
        return getIntPageParameter(parameterName, -1);
    }

    public static int getIntPageParameter(String parameterName, int defaultValue) {
        try {
            return Integer.parseInt(getPageParameters().get(parameterName));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Возвращает значение аргумента на странице, которое можно настроить на сервере.
     * Делается через определение статического javascript объекта на странице с предопределённым именем: pageSetup
     */
    public static String getPageSetupArgument(String argumentName) {
        Dictionary setupDict = Dictionary.getDictionary("pageSetup");
        try {
            return setupDict != null ? setupDict.get(argumentName) : null;
        } catch (MissingResourceException e) {
            //если аргумент не найден, то возвращаем null
            return null;
        }
    }

    public static String getWebAppBaseURL() {
        String webAppRoot = getPageSetupArgument("webAppRoot");
        return webAppRoot != null ? webAppRoot : GWT.getHostPageBaseURL();
    }

    public static String getAbsoluteUrl(String relativeUrl) {
        String absoluteUrl = GwtClientUtils.getWebAppBaseURL() + relativeUrl;
        if (!GWT.isScript()) {
            absoluteUrl += "?" + GWT_DEVMODE_PARAM + "=" + Window.Location.getParameter(GWT_DEVMODE_PARAM);
        }
        return absoluteUrl;
    }

    public static void stopPropagation(NativeEvent event) {
        event.stopPropagation();
        event.preventDefault();
    }

    public static void stopPropagation(DomEvent event) {
        event.stopPropagation();
        event.preventDefault();
    }

    public static void removeAllChildren(Element parent) {
        parent.setInnerText("");
    }

    public static Widget createHorizontalStrut(int size) {
        SimplePanel strut = new SimplePanel();
        strut.setWidth(size + "px");
        return strut;
    }

    public static String getUserAgent() {
        return Window.Navigator.getUserAgent().toLowerCase();
    };

    public static boolean isIEUserAgent() {
        return getUserAgent().contains("msie");
    }

    public static boolean isVisible(Widget w) {
        if (w.getParent() == null) {
            return w.isVisible();
        }
        return w.isVisible() && isVisible(w.getParent());
    }

    public static void setupFillParent(Element parent, Element child) {
        parent.getStyle().setPosition(Style.Position.RELATIVE);
        setupFillParent(child);
    }

    public static void setupFillParent(Element child) {
        Style childStyle = child.getStyle();
        childStyle.setPosition(Style.Position.ABSOLUTE);
        childStyle.setTop(0, Style.Unit.PX);
        childStyle.setLeft(0, Style.Unit.PX);
        childStyle.setBottom(0, Style.Unit.PX);
        childStyle.setRight(0, Style.Unit.PX);
    }

    public static Dimension getOffsetSize(Widget widget) {
        return new Dimension(widget.getElement().getOffsetWidth(), widget.getElement().getOffsetHeight());
    }

    public static Dimension maybeGetPreferredSize(Widget widget) {
        return maybeGetPreferredSize(widget, true);
    }

    public static Dimension maybeGetPreferredSize(Widget widget, boolean useOffsetSize) {
        if (widget instanceof HasPreferredSize) {
            return ((HasPreferredSize) widget).getPreferredSize();
        } else if (useOffsetSize) {
            return new Dimension(widget.getOffsetWidth(), widget.getOffsetHeight());
        }
        return new Dimension(0, 0);
    }

    public static Dimension calculateStackPreferredSize(Iterator<Widget> widgets, boolean vertical) {
        return calculateStackPreferredSize(widgets, vertical, false);
    }

    public static Dimension calculateStackPreferredSize(Iterator<Widget> widgets, boolean vertical, boolean useOffsetSize) {
        int width = 0;
        int height = 0;
        while (widgets.hasNext()) {
            Widget childView = widgets.next();
            if (childView.isVisible()) {
                Dimension childSize = maybeGetPreferredSize(childView, useOffsetSize);
                if (vertical) {
                    width = max(width, childSize.width);
                    height += childSize.height;
                } else {
                    width += childSize.width;
                    height = max(height, childSize.height);
                }
            }
        }
        return new Dimension(width, height);
    }
}
