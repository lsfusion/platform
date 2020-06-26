package lsfusion.gwt.client.base;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.BodyElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.http.client.*;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.view.HasMaxPreferredSize;
import lsfusion.gwt.client.view.MainFrame;

import java.util.*;
import java.util.function.Consumer;

import static java.lang.Math.max;
import static lsfusion.gwt.client.view.MainFrame.colorTheme;

public class GwtClientUtils {

    private static final ClientMessages messages = ClientMessages.Instance.get();
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
        return getWebAppBaseURL() + pageUrl + Window.Location.getQueryString();
    }

    public static String getLogoutUrl() {
        return getPageUrlPreservingParameters("logout");
    }

    public static void reconnect() {
        MainFrame.disableConfirmDialog = true;
        Window.Location.reload();
    }

    public static void logout() {
        logout(false);
    }

    public static void logout(boolean connectionLost) {
        MainFrame.disableConfirmDialog = true;
        //CloseNavigator should be called before logout, because logout removes authentication
        MainFrame.cleanRemote(() -> Window.open(GwtClientUtils.getLogoutUrl(), "_self", null), connectionLost);
    }

    public static void downloadFile(String name, String displayName, String extension) {
        if (name != null) {
            String fileUrl = getDownloadURL(name, displayName, extension, true);
            Window.open(fileUrl, "_blank", ""); // displayName != null ? displayName : name
        }
    }

    public static String getDownloadURL(String name, String displayName, String extension, boolean actionFile) {
        return getWebAppBaseURL() + GwtSharedUtils.getDownloadURL(name, displayName, extension, actionFile);
    }

    public static String getModuleImagePath(String imagePath) {
        return imagePath == null ? null : GWT.getModuleBaseURL() + "static/images/" + imagePath;
    }

    public static String getAppImagePath(String imagePath) {
        return imagePath == null ? null : getWebAppBaseURL() + imagePath;
    }

    private static Map<String, Boolean> imagePathCache = new HashMap<>();

    public static void setThemeImage(String imagePath, Consumer<String> modifier) {
        setThemeImage(imagePath, modifier, true);
    }

    public static void setThemeImage(String imagePath, Consumer<String> modifier, boolean isEnableColorTheme) {
        if (imagePath != null && !colorTheme.isDefault() && isEnableColorTheme) {
            String colorThemeImagePath = MainFrame.colorTheme.getImagePath(imagePath);
            GwtClientUtils.ensureImage(colorThemeImagePath, new Callback() {
                @Override
                public void onFailure() {
                    modifier.accept(getModuleImagePath(imagePath));
                }

                @Override
                public void onSuccess() {
                    modifier.accept(getModuleImagePath(colorThemeImagePath));
                }
            });
        } else {
            modifier.accept(getModuleImagePath(imagePath));
        }
    }


    public static void ensureImage(String imagePath, Callback callback) {
        Boolean cachedResult = imagePathCache.get(imagePath);
        if (cachedResult != null) {
            if (cachedResult) {
                callback.onSuccess();
            } else {
                callback.onFailure();
            }
        } else {
            try {
                RequestBuilder rb = new RequestBuilder(RequestBuilder.HEAD, getModuleImagePath(imagePath));
                rb.setCallback(new RequestCallback() {
                    @Override
                    public void onResponseReceived(Request request, Response response) {
                        if (response.getStatusCode() > 199 && response.getStatusCode() < 300) {
                            imagePathCache.put(imagePath, true);
                            callback.onSuccess();
                        } else {
                            imagePathCache.put(imagePath, false);
                            callback.onFailure();
                        }
                    }

                    @Override
                    public void onError(Request request, Throwable exception) {
                        imagePathCache.put(imagePath, false);
                        callback.onFailure();
                    }
                });
                rb.send();
            } catch (RequestException ignored) {
                callback.onFailure();
            }
        }
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

    public static void setAttemptCount(int attemptCount) {
        RootPanel p = RootPanel.get("loadingMsg");
        if (p != null) {
            p.getElement().setInnerHTML(attemptCount == 1 ? messages.rmiConnectionLostMessage(attemptCount) : messages.rmiConnectionLostMessageUnavailable(attemptCount));
        }
    }

    public static String getWebAppBaseURL() {
        String webAppRoot = getPageSetupArgument("webAppRoot");
        return webAppRoot != null ? webAppRoot : GWT.getHostPageBaseURL();
    }

    public static String getLogicsName() {
        String logicsName = getPageSetupArgument("logicsName");
        return logicsName != null ? logicsName : "default";
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

    public static Widget createVerticalSeparator(int height) {
        SimplePanel separator = new SimplePanel();
        separator.setHeight(height + "px");
        separator.addStyleName("verticalSeparator");
        return separator;
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

    // using absolute positioning, but because in that case it is positioned relative to first not static element, will have to set position to relative (if it's static)
    public static void setupFillParent(Element child) {
        Element parentElement = child.getParentElement();
        String parentPosition = parentElement.getStyle().getPosition();
        if (parentPosition == null || parentPosition.isEmpty() || parentPosition.equals(Style.Position.STATIC.getCssName()))
            parentElement.getStyle().setPosition(Style.Position.RELATIVE);

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

    public static void showPopupInWindow(PopupPanel popup, int mouseX, int mouseY) {
        // special minimum offset for Firefox, where PopupPanel swallows MOUSEOVER event 
        popup.setPopupPosition(mouseX + 1, mouseY + 1);
        popup.show();

        int popupWidth = popup.getOffsetWidth();
        int popupHeight = popup.getOffsetHeight();
        int xCorrection = popupWidth - (Window.getClientWidth() - mouseX);
        int yCorrection = popupHeight - (Window.getClientHeight() - mouseY);

        if (xCorrection > 0 || yCorrection > 0) {
            if (xCorrection > 0 && yCorrection > 0) {
                // For the same reason with a lack of space on both sides (right and bottom) we show popup on the opposite side of the cursor. 
                // Otherwise, in Firefox we won't see the popup at all.
                popup.setPopupPosition(mouseX - popupWidth, mouseY - popupHeight);
            } else {
                popup.setPopupPosition(
                        xCorrection > 0 ? max(mouseX - xCorrection, 0) : mouseX + 1,
                        yCorrection > 0 ? max(mouseY - yCorrection, 0) : mouseY + 1
                );
            }
        }
    }

    /**
     * should always be consistent with lsfusion.client.form.property.table.view.TableTransferHandler#getClipboardTable(java.lang.String)
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
        if (equals(reduce, 0.0))
            return prevFlexWidth;

        double newFlexWidth = prevFlexWidth + reduce;
        double newTotalFlexes = 0.0;
        double prevTotalFlexes = 0.0;
        for (int i = 0; i < prefs.length; i++) {
            if (i != column) {
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
        while (column >= 0 && baseFlexes[column] == 0)
            column--;
        if (column < 0) // нет левой flex колонки - ничего не делаем
            return;

        int rightFlex = column + 1;
        while (rightFlex < baseFlexes.length && baseFlexes[rightFlex] == 0)
            rightFlex++;
        if (rightFlex >= baseFlexes.length) // не нашли правй flex - ничего не делаем
            return;

        // считаем общий текущий preferred
        double totalPref = 0;
        for (double pref : prefs) {
            totalPref += pref;
        }

        // сначала списываем delta справа налево pref (но не меньше basePref), ПОКА сумма pref > viewWidth !!! ( то есть flex не работает, работает ширина контейнера или minTableWidth в таблице)
        // тут можно было бы если идет расширение - delta > 0.0, viewWidth приравнять totalPref (соответственно запретить adjust, то есть pref'ы остались такими же) и reduce'ить остальные, но это пойдет в разрез с уменьшением (когда нужно уменьшать pref'ы иначе в исходное состояние не вернешься), поэтому логичнее исходить из концепции когда если есть scroll тогда просто расширяем колонки, если нет scroll'а пытаемся уместить все без скролла
        double exceedPrefWidth = totalPref - viewWidth;
        if (greater(exceedPrefWidth, 0.0)) {
            if (!overflow)
                return;

            double prefReduceDelta = Math.min(-delta, exceedPrefWidth);
            delta += prefReduceDelta;
            for (int i = column; i >= 0; i--) {
                double maxReduce = prefs[i] - basePrefs[i];
                double reduce = Math.min(prefReduceDelta, maxReduce);
                prefs[i] -= reduce;
                prefReduceDelta -= reduce;
                if (!removeLeftPref || equals(prefReduceDelta, 0.0)) // если delta не осталось нет смысла продолжать, у нас либо viewWidth либо уже все расписали
                    break;
            }

            assert greaterEquals(0.0, delta);

            exceedPrefWidth = 0;
        }

        if (delta == 0) // все расписали
            return;

        double flexWidth = -exceedPrefWidth;
        assert greaterEquals(flexWidth, 0.0);

        // можно переходить на basePref - flex (с учетом того что viewWidth может измениться, pref'ы могут быть как равны viewWidth в результате предыдущего шага, так и меньше)
        for (int i = 0; i < prefs.length; i++)
            flexWidth = reducePrefsToBase(flexWidth, i, prefs, flexes, basePrefs);

        //если flexWidth все еще равно 0 - вываливаемся (так как нельзя меньше preferred опускаться)
        if (equals(flexWidth, 0.0))
            return;

        // запускаем изменение flex'а (пропорциональное)
        double totalFlex = 0;
        double totalBaseFlex = 0;
        double totalRightFlexes = 0.0;
        double totalRightBaseFlexes = 0.0;
        for (int i = 0; i < flexes.length; i++) {
            double flex = flexes[i];
            double baseFlex = baseFlexes[i];
            if (i > column) {
                totalRightFlexes += flex;
                totalRightBaseFlexes += baseFlex;
            }
            totalFlex += flex;
            totalBaseFlex += baseFlex;
        }

        // flex колонки увеличиваем на нужную величину, соответственно остальные flex'ы надо уменьшить на эту величину
        double toAddFlex = (double) delta * totalFlex / (double) flexWidth;
        if (greater(0.0, toAddFlex + flexes[column])) // не shrink'аем, но и левые столбцы не уменьшаются (то есть removeLeftFlex false)
            toAddFlex = -flexes[column];

        // сначала уменьшаем правые flex'ы
        double restFlex = 0.0;
        double toAddRightFlex = toAddFlex;
        if (toAddRightFlex > totalRightFlexes) {
            restFlex = toAddRightFlex - totalRightFlexes;
            toAddRightFlex = totalRightFlexes;
        }
        for (int i = column + 1; i < flexes.length; i++) {
            if (greater(totalRightFlexes, 0.0))
                flexes[i] -= flexes[i] * toAddRightFlex / totalRightFlexes;
            else {
                assert equals(flexes[i], 0.0);
                flexes[i] = -baseFlexes[i] * toAddRightFlex / totalRightBaseFlexes;
            }
        }


        // может остаться delta, тогда раскидываем ее для левых компонент
        boolean addLeftFlex = !overflow; // (если не overflow, потому как в противном случае все же не очень естественное поведение)
        if (addLeftFlex && greater(restFlex, 0.0)) {
            double totalLeftFlexes = totalFlex - totalRightFlexes - flexes[column];
            double totalLeftBaseFlexes = totalBaseFlex - totalRightBaseFlexes - baseFlexes[column];

            double toAddLeftFlex = restFlex; // надо изменять preferred - то есть overflow'ить / добавлять scroll по сути
            restFlex = 0.0;
            if (toAddLeftFlex > totalLeftFlexes) {
                restFlex = toAddLeftFlex - totalLeftFlexes;
                toAddLeftFlex = totalLeftFlexes;
            }
            for (int i = 0; i < column; i++) {
                if (greater(totalLeftFlexes, 0.0))
                    flexes[i] -= flexes[i] * toAddLeftFlex / totalLeftFlexes;
                else {
                    assert equals(flexes[i], 0.0);
                    flexes[i] = -baseFlexes[i] * toAddLeftFlex / totalLeftBaseFlexes;
                }
            }
        }

        toAddFlex = toAddFlex - restFlex;
        flexes[column] += toAddFlex;

        // если и так осталась, то придется давать preferred (соответственно flex не имеет смысла) и "здравствуй" scroll
        if (greater(restFlex, 0.0)) {
            assert !addLeftFlex || equals(flexes[column], totalFlex); // по сути записываем все в эту колонку
            if (overflow) {
                if (!addLeftFlex) {
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
        for (int i = 0; i < prefs.length; i++) {
            if (flexes[i]) {
                double ratio = flexValues[i] / prefs[i];
                minRatio = Math.min(minRatio, ratio);
                totalFlexValues += flexValues[i];
            }
            totalPref += prefs[i];
        }
        double flexWidth = Math.max((double) viewWidth - totalPref, 0.0);
        for (int i = 0; i < prefs.length; i++) {
            if (flexes[i])
                prefs[i] = (prefs[i] + flexWidth * flexValues[i] / totalFlexValues) / (1.0 + flexWidth * minRatio / totalFlexValues);
        }
    }

    // изменяется prefs
    public static void calculateNewFlexesForFixedTableLayout(int column, int delta, int viewWidth, double[] prefs, int[] basePrefs, boolean[] flexes) {
        double[] flexValues = new double[prefs.length];
        double[] baseFlexValues = new double[prefs.length];
        for (int i = 0; i < prefs.length; i++) {
            if (flexes[i]) {
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

    //equal to BaseUtils.replaceSeparators
    public static String replaceSeparators(String value, String separator, String groupingSeparator) {
        if (value.contains(",") && !groupingSeparator.equals(",") && separator.equals("."))
            value = replaceCommaSeparator(value);
        else if (value.contains(".") && !groupingSeparator.equals(".") && separator.equals(","))
            value = replaceDotSeparator(value);
        return value;
    }

    //equal to BaseUtils.replaceCommaSeparator
    public static String replaceCommaSeparator(String value) {
        return value.replace(',', '.');
    }

    //equal to BaseUtils.replaceDotSeparator
    public static String replaceDotSeparator(String value) {
        return value.replace('.', ',');
    }

    public static String replicate(char character, int length) {
        char[] chars = new char[length];
        Arrays.fill(chars, character);
        return new String(chars);
    }
    
    public static String getCurrentLocaleName() {
        return LocaleInfo.getCurrentLocale().getLocaleName();
    } 
    
    public static String getCurrentLanguage() {
        return getCurrentLocaleName().substring(0, 2);
    }

    public static native Element getFocusedElement() /*-{
        return $doc.activeElement;
    }-*/;

    public static Element getFocusedChild(Element containerElement) {
        Element focusedElement = getFocusedElement();
        Element element = focusedElement;
        BodyElement body = Document.get().getBody();
        while (element != body) {
            if(element == containerElement)
                return focusedElement;
            element = element.getParentElement().cast();
        }
        return null;
    }

}
