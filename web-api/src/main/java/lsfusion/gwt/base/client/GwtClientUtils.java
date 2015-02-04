package lsfusion.gwt.base.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.http.client.URL;
import com.google.gwt.http.client.UrlBuilder;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.base.client.ui.HasPreferredSize;
import lsfusion.gwt.base.shared.GwtSharedUtils;

import java.util.*;

import static java.lang.Math.max;

public class GwtClientUtils {
    public static final String TIMEOUT_MESSAGE = "SESSION_TIMED_OUT";
    public static final String TARGET_URL_PARAM = "targetUrl";
    public static final String GWT_DEVMODE_PARAM = "gwt.codesvr";

    public static final BaseMessages baseMessages = BaseMessages.Instance.get();
    public static final com.google.gwt.user.client.Element rootElement = RootPanel.get().getElement();

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

    public static Widget createVerticalStrut(int size) {
        SimplePanel strut = new SimplePanel();
        strut.setHeight(size + "px");
        return strut;
    }

    public static String getUserAgent() {
        return Window.Navigator.getUserAgent().toLowerCase();
    }

    public static boolean isIEUserAgent() {
        return getUserAgent().contains("msie");
    }

    public static boolean isShowing(Widget widget) {
        if (widget == null) {
            return false;
        }
        Element el = widget.getElement();
        while (el != null && el != rootElement) {
            if (!UIObject.isVisible(el)) {
                return false;
            }

            el = el.getParentElement();
        }

        return el == rootElement;
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
        return getOffsetSize(widget, 0, 0);
    }

    public static Dimension getOffsetSize(Widget widget, int widthExtra, int heightExtra) {
        return new Dimension(widget.getOffsetWidth() + widthExtra, widget.getOffsetHeight() + heightExtra);
    }

    public static Dimension calculatePreferredSize(Widget widget) {
        if (widget instanceof HasPreferredSize) {
            return ((HasPreferredSize) widget).getPreferredSize();
        } else {
            return new Dimension(widget.getOffsetWidth(), widget.getOffsetHeight());
        }
    }

    public static Dimension calculateStackPreferredSize(Iterator<Widget> widgets, boolean vertical) {
        int width = 0;
        int height = 0;
        while (widgets.hasNext()) {
            Widget childView = widgets.next();
            if (childView.isVisible()) {
                Dimension childSize = calculatePreferredSize(childView);
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

    public static Dimension enlargeDimension(Dimension dim, int extraWidth, int extraHeight) {
        dim.width += extraWidth;
        dim.height += extraHeight;
        return dim;
    }

    public static void installPaddings(Element element, int paddingTop, int paddingBottom, int paddingLeft, int paddingRight) {
        Style style = element.getStyle();
        style.setPaddingTop(paddingTop, Style.Unit.PX);
        style.setPaddingBottom(paddingBottom, Style.Unit.PX);
        style.setPaddingLeft(paddingLeft, Style.Unit.PX);
        style.setPaddingRight(paddingRight, Style.Unit.PX);
    }

    public static void scheduleOnResize(final Widget widget) {
        if (widget instanceof RequiresResize) {
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    ((RequiresResize) widget).onResize();
                }
            });
        }
    }

    /**
     * should always be consistent with lsfusion.client.form.TableTransferHandler#getClipboardTable(java.lang.String)
     */
    public static List<List<String>> getClipboardTable(String line) {
        List<List<String>> table = new ArrayList<List<String>>();
        List<String> row = new ArrayList<String>();

        char[] charline = line.toCharArray();

        int quotesCount = 0;
        boolean quotesOpened = false;
        boolean hasSeparator = false;
        boolean isFirst = true;

        int start = 0;

        for (int i = 0; i <= charline.length; i++) {
            boolean isCellEnd, isRowEnd, isQuote = false, isSeparator;

            boolean isLast = i >= charline.length;
            if (!isLast) {
                isCellEnd = charline[i] == '\t';
                isRowEnd = charline[i] == '\n';
                isQuote = charline[i] == '"';
                isSeparator = isCellEnd || isRowEnd;
            } else {
                if (isFirst)
                    break;
                isRowEnd = true;
                isSeparator = true;
            }

            if (quotesOpened) {
                if (isQuote) {
                    quotesCount++;
                } else {
                    if (isSeparator) {
                        if (quotesCount % 2 == 0 || isLast) {
                            String cell = line.substring(hasSeparator ? (start + 1) : start, hasSeparator ? (i - 1) : i);
                            row.add(GwtSharedUtils.nullEmpty(cell));
                            if (isRowEnd) {
                                table.add(row);
                                row = new ArrayList<String>();
                            }

                            start = i;
                            quotesOpened = false;
                            isFirst = true;
                            hasSeparator = false;
                        } else {
                            hasSeparator = true;
                        }
                    }
                }
            } else if (isSeparator) {
                row.add(GwtSharedUtils.nullEmpty(line.substring(start, i)));
                if (isRowEnd) {
                    table.add(row);
                    row = new ArrayList<String>();
                }

                start = i;
                isFirst = true;
            } else if (isFirst) {
                if (isQuote) {
                    quotesOpened = true;
                    quotesCount = 1;
                }
                start = i;
                isFirst = false;
            }
        }

        if (table.isEmpty()) {
            row.add(null);
            table.add(row);
        }

        return table;
    }
}
