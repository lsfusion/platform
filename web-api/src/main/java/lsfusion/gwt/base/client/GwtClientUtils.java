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
import lsfusion.gwt.base.client.ui.HasMaxPreferredSize;
import lsfusion.gwt.base.shared.GwtSharedUtils;

import java.util.*;

import static java.lang.Math.PI;
import static java.lang.Math.max;
import static java.lang.Math.min;

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
        return getPageUrlPreservingParameters("logout");
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
        Map<String, String> params = new HashMap<>();
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
        String userAgent = getUserAgent();
        // надо бы как-то покрасивее определять браузер
        return userAgent.contains("msie") || 
                (userAgent.contains("rv:11.0") && !userAgent.contains("firefox")) || 
                userAgent.contains("edge");
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

    public static Dimension calculateMaxPreferredSize(Widget widget) { // тут как и в AbstractClientContainerView.getMaxPreferredSize возможно нужна проверка на isVisible
        if (widget instanceof HasMaxPreferredSize) {
            return ((HasMaxPreferredSize) widget).getMaxPreferredSize();
        } else {
            return new Dimension(widget.getOffsetWidth(), widget.getOffsetHeight());
        }
    }

    public static Dimension calculateStackMaxPreferredSize(Iterator<Widget> widgets, boolean vertical) {
        int width = 0;
        int height = 0;
        while (widgets.hasNext()) {
            Widget childView = widgets.next();
            if (childView.isVisible()) {
                Dimension childSize = calculateMaxPreferredSize(childView);
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
        List<List<String>> table = new ArrayList<>();
        List<String> row = new ArrayList<>();

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
                                row = new ArrayList<>();
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
                    row = new ArrayList<>();
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

    // возвращает новую flexWidth
    private static double reducePrefsToBase(double prevFlexWidth, int column, double[] prefs, double[] flexes, int[] basePrefs) {
        double reduce = prefs[column] - basePrefs[column];
        assert greaterEquals(reduce, 0.0);
        if(equals(reduce, 0.0))
            return prevFlexWidth;

        double newFlexWidth = prevFlexWidth + reduce;
        double newTotalFlexes = 0.0;
        double prevTotalFlexes = 0.0;
        for(int i=0;i<prefs.length;i++) {
            if(i!=column) {
                double prevFlex = flexes[i];
                double newFlex = prevFlex * prevFlexWidth / newFlexWidth;
                flexes[i] = newFlex;
                newTotalFlexes += newFlex;
                prevTotalFlexes += prevFlex;
            }
        }
        assert newTotalFlexes < prevTotalFlexes;
        flexes[column] += prevTotalFlexes - newTotalFlexes;
        prefs[column] = basePrefs[column];
        return newFlexWidth;
    }

    private static boolean greater(double a, double b) {
        return a - b > 0.001;
    }
    private static boolean greaterEquals(double a, double b) {
        return a - b > -0.001;
    }
    private static boolean equals(double a, double b) {
        return Math.abs(a - b) < 0.001;
    }

    // prefs на double'ах чтобы не "дрожало", из-за преобразований в разные стороны (строго говоря наверное без adjustTableFixed и overflow дрожать не будет)
    public static void calculateNewFlexes(int column, int delta, int viewWidth, double[] prefs, double[] flexes, int[] basePrefs, double[] baseFlexes, boolean overflow) {
        boolean removeLeftPref = true; // вообще так как removeLeftFlex false, логично иметь симметричное поведение, но больше не меньше (removeRightPref и add*Pref не имеют смысла, так как вся delta просто идет в pref колонки)
        
        // ищем первую динамическую компоненту слева (она должна получить +delta, соответственно правая часть -delta)
        // тут есть варианты -delta идет одной правой колонке, или всем правых колонок, но так как
        // a) так как выравнивание по умолчанию левое, интуитивно при перемещении изменяют именно размер левой колонки, б) так как есть де-факто ограничение Preferred, вероятность получить нужный размер уменьшая все колонки куда выше
        // будем распределять между всеми правыми колонками

        // находим левую flex
        while(column >= 0 && baseFlexes[column] == 0)
            column--;
        if(column < 0) {
            while(column < baseFlexes.length && baseFlexes[column] == 0)
                column++;
            if(column >= baseFlexes.length) // вообще нет ни одной динамической колонки - ничего не меняем
                return;
        }

        // считаем общий текущий preferred
        double totalPref = 0;
        for (double pref : prefs) {
            totalPref += pref;
        }

        // сначала списываем delta справа налево pref (но не меньше basePref), ПОКА сумма pref > viewWidth !!! ( то есть flex не работает, работает ширина контейнера или minTableWidth в таблице)
        // тут можно было бы если идет расширение - delta > 0.0, viewWidth приравнять totalPref (соответственно запретить adjust, то есть pref'ы остались такими же) и reduce'ить остальные, но это пойдет в разрез с уменьшением (когда нужно уменьшать pref'ы иначе в исходное состояние не вернешься), поэтому логичнее исходить из концепции когда если есть scroll тогда просто расширяем колонки, если нет scroll'а пытаемся уместить все без скролла
        double exceedPrefWidth = totalPref - viewWidth;
        if(greater(exceedPrefWidth, 0.0)) {
            if(!overflow)
                return;

            double prefReduceDelta = Math.min(-delta, exceedPrefWidth);
            delta += prefReduceDelta;
            for(int i=column;i>=0;i--) {
                double maxReduce = prefs[i] - basePrefs[i];
                double reduce = Math.min(prefReduceDelta, maxReduce);
                prefs[i] -= reduce;
                prefReduceDelta -= reduce;
                if(!removeLeftPref || equals(prefReduceDelta, 0.0)) // если delta не осталось нет смысла продолжать, у нас либо viewWidth либо уже все расписали
                    break;
            }

            assert greaterEquals(0.0, delta);

            exceedPrefWidth = 0;
        }

        if(delta == 0) // все расписали
            return;

        double flexWidth = -exceedPrefWidth;
        assert greaterEquals(flexWidth, 0.0);

        // можно переходить на basePref - flex (с учетом того что viewWidth может измениться, pref'ы могут быть как равны viewWidth в результате предыдущего шага, так и меньше)
        for(int i=0;i<prefs.length;i++)
            flexWidth = reducePrefsToBase(flexWidth, i, prefs, flexes, basePrefs);

        //если flexWidth все еще равно 0 - вываливаемся (так как нельзя меньше preferred опускаться)
        if(equals(flexWidth,0.0))
            return;

        // запускаем изменение flex'а (пропорциональное)
        double totalFlex = 0;
        double totalBaseFlex = 0;
        double totalRightFlexes = 0.0;
        double totalRightBaseFlexes = 0.0;
        for(int i=0;i<flexes.length;i++) {
            double flex = flexes[i];
            double baseFlex = baseFlexes[i];
            if(i>column) {
                totalRightFlexes += flex;
                totalRightBaseFlexes += baseFlex;
            }
            totalFlex += flex;
            totalBaseFlex += baseFlex;
        }

        // flex колонки увеличиваем на нужную величину, соответственно остальные flex'ы надо уменьшить на эту величину
        double toAddFlex = (double) delta * totalFlex / (double) flexWidth;
        if(greater(0.0, toAddFlex + flexes[column])) // не shrink'аем, но и левые столбцы не уменьшаются (то есть removeLeftFlex false)
            toAddFlex = -flexes[column];

        // сначала уменьшаем правые flex'ы
        double restFlex = 0.0;
        double toAddRightFlex = toAddFlex;
        if(toAddRightFlex > totalRightFlexes) {
            restFlex = toAddRightFlex - totalRightFlexes;
            toAddRightFlex = totalRightFlexes;
        }
        for(int i=column+1;i<flexes.length;i++) {
            if(greater(totalRightFlexes, 0.0))
                flexes[i] -= flexes[i] * toAddRightFlex / totalRightFlexes;
            else {
                assert equals(flexes[i], 0.0);
                flexes[i] = - baseFlexes[i] * toAddRightFlex / totalRightBaseFlexes;
            }
        }


        // может остаться delta, тогда раскидываем ее для левых компонент
        boolean addLeftFlex = !overflow; // (если не overflow, потому как в противном случае все же не очень естественное поведение)
        if(addLeftFlex && greater(restFlex, 0.0)) {
            double totalLeftFlexes = totalFlex - totalRightFlexes - flexes[column];
            double totalLeftBaseFlexes = totalBaseFlex - totalRightBaseFlexes - baseFlexes[column];

            double toAddLeftFlex = restFlex; // надо изменять preferred - то есть overflow'ить / добавлять scroll по сути
            restFlex = 0.0;
            if(toAddLeftFlex > totalLeftFlexes) {
                restFlex = toAddLeftFlex - totalLeftFlexes;
                toAddLeftFlex = totalLeftFlexes;
            }
            for(int i=0;i<column;i++) {
                if(greater(totalLeftFlexes, 0.0))
                    flexes[i] -= flexes[i] * toAddLeftFlex / totalLeftFlexes;
                else {
                    assert equals(flexes[i], 0.0);
                    flexes[i] = - baseFlexes[i] * toAddLeftFlex / totalLeftBaseFlexes;
                }
            }
        }

        toAddFlex = toAddFlex - restFlex;
        flexes[column] += toAddFlex;

        // если и так осталась, то придется давать preferred (соответственно flex не имеет смысла) и "здравствуй" scroll
        if(greater(restFlex, 0.0)) {
            assert !addLeftFlex || equals(flexes[column], totalFlex); // по сути записываем все в эту колонку
            if(overflow) {
                if(!addLeftFlex) {
                    for (int i = 0; i < column; i++)
                        prefs[i] += flexWidth * flexes[i] / totalFlex;
                }
                prefs[column] += flexWidth * ((flexes[column] + restFlex) / totalFlex);
            }
        }
    }

    private static void adjustFlexesToFixedTableLayout(int viewWidth, double[] prefs, boolean[] flexes, double[] flexValues) {
        double minRatio = Double.MAX_VALUE;
        double totalPref = 0;
        double totalFlexValues = 0.0;
        for(int i=0;i<prefs.length;i++) {
            if(flexes[i]) {
                double ratio = flexValues[i] / prefs[i];
                minRatio = Math.min(minRatio, ratio);
                totalFlexValues += flexValues[i];
            }
            totalPref += prefs[i];
        }
        double flexWidth = Math.max((double)viewWidth - totalPref, 0.0);
        for(int i=0;i<prefs.length;i++) {
            if(flexes[i])
                prefs[i] = (prefs[i] + flexWidth * flexValues[i] / totalFlexValues) / (1.0 + flexWidth * minRatio / totalFlexValues);
        }
    }

    // изменяется prefs
    public static void calculateNewFlexesForFixedTableLayout(int column, int delta, int viewWidth, double[] prefs, int[] basePrefs, boolean[] flexes) {
        double[] flexValues = new double[prefs.length];
        double[] baseFlexValues = new double[prefs.length];
        for(int i=0;i<prefs.length;i++) {
            if(flexes[i]) {
                flexValues[i] = prefs[i];
                baseFlexValues[i] = basePrefs[i];
            } else {
                flexValues[i] = 0.0;
                baseFlexValues[i] = 0.0;
            }
        }

        calculateNewFlexes(column, delta, viewWidth, prefs, flexValues, basePrefs, baseFlexValues, true);

        adjustFlexesToFixedTableLayout(viewWidth, prefs, flexes, flexValues);
    }
}
