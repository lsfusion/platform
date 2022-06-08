package lsfusion.gwt.client.base;

import com.google.gwt.core.client.*;
import com.google.gwt.dom.client.*;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.lambda.EFunction;
import lsfusion.gwt.client.base.view.PopupDialogPanel;
import lsfusion.gwt.client.view.MainFrame;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.lang.Math.max;
import static lsfusion.gwt.client.view.MainFrame.colorTheme;

public class GwtClientUtils {

    private static final ClientMessages messages = ClientMessages.Instance.get();
    public static final com.google.gwt.user.client.Element rootElement = RootPanel.get().getElement();

    public final static String UNBREAKABLE_SPACE = "\u00a0";

    public static void removeLoaderFromHostedPage() {
        RootPanel p = RootPanel.get("loadingWrapper");
        if (p != null) {
            RootPanel.getBodyElement().removeChild(p.getElement());
        }
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

    public static void downloadFile(String name, String displayName, String extension, boolean autoPrint, Integer autoPrintTimeout) {
        if (name != null) {
            JavaScriptObject window = openWindow(getAppDownloadURL(name, displayName, extension));
            if (autoPrint)
                print(window, autoPrintTimeout);
        }
    }

    public static native JavaScriptObject openWindow(String url)/*-{
        return $wnd.open(url);
    }-*/;

    public static native void print(JavaScriptObject window, Integer timeout)/*-{
        window.onload = function () {
            window.print();

            //We can not append any script to the PDF window because it is from MIME type "application/pdf" and detect the print events only for HTML-documents
            if (timeout != null)
                setTimeout(function closeWindow() { window.close(); }, timeout);
        }
        window.onafterprint = function () {
            window.close();
        }
    }-*/;

    private static String getDownloadParams(String displayName, String extension) {
        String params = "";
        if(displayName != null)
            params = params + "&displayName=" + displayName;
        if(extension != null)
            params = params + "&extension=" + extension;
        return params;
    }

    private static String getURL(String url) {
        return url == null ? null : getWebAppBaseURL() + url;
    }

    // the one that is in gwt(main)/public/static/images
    // FileUtils.STATIC_IMAGE_FOLDER_PATH
    public static String getStaticImageURL(String imagePath) {
        if(imagePath != null && MainFrame.staticImagesURL == null)
            return GWT.getModuleBaseURL() + "static/images/" + imagePath;
        return getURL(imagePath != null ? MainFrame.staticImagesURL + imagePath : null); // myapp/imagepath
//        return imagePath == null ? null : GWT.getModuleBaseURL() + "static/images/" + imagePath; // myapp/main/static/images/myimage/imagepath
    }
    // the on that is in the app server resources
    public static String getAppStaticImageURL(String imagePath) {
        return getURL(imagePath);
    }

    public static String getAppStaticWebURL(String filePath) {
        return getURL(filePath);
    }
    // FileUtils.APP_DOWNLOAD_FOLDER_PATH
    public static String getAppDownloadURL(String url, String displayName, String extension) {
        assert url != null;
        return getURL(url + getDownloadParams(displayName, extension));
    }

    // FileUtils.APP_UPLOAD_FOLDER_PATH
    public static String getUploadURL(String fileName) {
        return getURL("uploadFile" + (fileName != null ? "?sid=" + fileName : ""));
    }

    public static void setThemeImage(String imagePath, Consumer<String> modifier) {
        if (imagePath != null && !colorTheme.isDefault()) {
            modifier.accept(getStaticImageURL(colorTheme.getImagePath(imagePath)));
        } else {
            modifier.accept(getStaticImageURL(imagePath));

            if(MainFrame.staticImagesURL == null)
                MainFrame.staticImagesURLListeners.add(() -> setThemeImage(imagePath, modifier));
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
        stopPropagation(event, false, false);
    }

    public static void stopPropagation(NativeEvent event, boolean propagateToNative, boolean propagateToUpper) {
        if(!propagateToUpper)
            event.stopPropagation();
        if(!propagateToNative)
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

    public static Widget createVerticalStretchSeparator() {
        SimplePanel separator = new SimplePanel();
        separator.addStyleName("verticalStretchSeparator");
        return separator;
    }

    public static Widget createVerticalSeparator(int height) {
        SimplePanel separator = new SimplePanel();
        separator.setHeight(height + "px");
        separator.addStyleName("verticalSeparator");
        return separator;
    }

    public static Widget createHorizontalSeparator() {
        SimplePanel separator = new SimplePanel();
        separator.addStyleName("horizontalSeparator");
        return separator;
    }

    public static String createTooltipHorizontalSeparator() {
        Widget separator = createHorizontalSeparator();
        separator.addStyleName("tooltipHorizontalSeparator");
        return separator.toString();
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

    public static void setupEdgeCenteredParent(Element child, boolean horz, boolean start) {
        Element parentElement = child.getParentElement();
        setupFillParentElement(parentElement);

        Style childStyle = child.getStyle();
        childStyle.setPosition(Style.Position.ABSOLUTE);
        if(horz) {
            if(start)
                childStyle.setLeft(0, Style.Unit.PX);
            else
                childStyle.setRight(0, Style.Unit.PX);

            childStyle.setTop(50, Style.Unit.PCT);
            childStyle.setProperty("transform", "translateY(-50%)");
        } else {
            if(start)
                childStyle.setTop(0, Style.Unit.PX);
            else
                childStyle.setBottom(0, Style.Unit.PX);

            childStyle.setLeft(50, Style.Unit.PCT);
            childStyle.setProperty("transform", "translateX(-50%)");
        }
    }

    public static void setupEdgeStretchParent(Element child, boolean horz, boolean start) {
        Element parentElement = child.getParentElement();
        setupFillParentElement(parentElement);

        Style childStyle = child.getStyle();
        childStyle.setPosition(Style.Position.ABSOLUTE);
        if(horz) {
            if(start)
                childStyle.setLeft(0, Style.Unit.PX);
            else
                childStyle.setRight(0, Style.Unit.PX);

            childStyle.setTop(0, Style.Unit.PX);
            childStyle.setBottom(0, Style.Unit.PX);
        } else {
            if(start)
                childStyle.setTop(0, Style.Unit.PX);
            else
                childStyle.setBottom(0, Style.Unit.PX);

            childStyle.setLeft(0, Style.Unit.PX);
            childStyle.setRight(0, Style.Unit.PX);
        }
    }

    // using absolute positioning, but because in that case it is positioned relative to first not static element, will have to set position to relative (if it's static)
    public static void setupFillParent(Element child) {
        setupFillParentElement(child.getParentElement());

        Style childStyle = child.getStyle();
        childStyle.setPosition(Style.Position.ABSOLUTE);
        childStyle.setTop(0, Style.Unit.PX);
        childStyle.setLeft(0, Style.Unit.PX);
        childStyle.setBottom(0, Style.Unit.PX);
        childStyle.setRight(0, Style.Unit.PX);
    }

    private static void setupFillParentElement(Element parentElement) {
        String parentPosition = parentElement.getStyle().getPosition();
        if (parentPosition == null || parentPosition.isEmpty() || parentPosition.equals(Style.Position.STATIC.getCssName()))
            parentElement.getStyle().setPosition(Style.Position.RELATIVE);
    }

    public static void clearFillParent(Element child) {
        clearFillParentElement(child.getParentElement());

        Style childStyle = child.getStyle();
        childStyle.clearPosition();
        childStyle.clearTop();
        childStyle.clearLeft();
        childStyle.clearBottom();
        childStyle.clearRight();
    }

    public static void clearFillParentElement(Element parentElement) {
        String parentPosition = parentElement.getStyle().getPosition();
        if (parentPosition != null && parentPosition.equals(Style.Position.RELATIVE.getCssName()))
            parentElement.getStyle().clearPosition();
    }

    public static void setupSizedParent(Element element, boolean autoSize) {
        if(autoSize)
            setupPercentParent(element);
        else
            setupFillParent(element);
    }

    public static void setupPercentParent(Element element) {
        element.getStyle().setWidth(100, Style.Unit.PCT);
        element.getStyle().setHeight(100, Style.Unit.PCT);
//        inputElement.addClassName("boxSized");
        element.getStyle().setProperty("boxSizing", "border-box");
    }

    public static void clearPercentParent(Element element) {
        element.getStyle().clearWidth();
        element.getStyle().clearHeight();
        element.getStyle().clearProperty("boxSizing");
    }

    public static void changePercentFillWidget(Widget widget, boolean percent) {
        if(percent) {
            GwtClientUtils.clearFillParent(widget.getElement());
            GwtClientUtils.setupPercentParent(widget.getElement());
        } else {
            GwtClientUtils.clearPercentParent(widget.getElement());
            GwtClientUtils.setupFillParent(widget.getElement());
        }
    }

    public static Dimension getOffsetSize(Widget widget) {
        return getOffsetSize(widget, 0, 0);
    }

    public static Dimension getOffsetSize(Widget widget, int widthExtra, int heightExtra) {
        return new Dimension(widget.getOffsetWidth() + widthExtra, widget.getOffsetHeight() + heightExtra);
    }

    public static void showPopupInWindow(PopupDialogPanel popup, Widget widget, int mouseX, int mouseY) {
        popup.setWidget(widget);

        showPopup(popup, mouseX, mouseY);

        Scheduler.get().scheduleDeferred(() -> widget.getElement().focus());
    }

    public static void showPopup(PopupDialogPanel popup, int mouseX, int mouseY) {
        popup.show();
        setPopupPosition(popup, mouseX, mouseY);
    }

    public static void setPopupPosition(PopupPanel popup, int mouseX, int mouseY) {
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
                        xCorrection > 0 ? max(mouseX - xCorrection, 0) : mouseX,
                        yCorrection > 0 ? max(mouseY - yCorrection, 0) : mouseY
                );
            }
        } else {
            popup.setPopupPosition(mouseX, mouseY);
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
        assert greaterEquals(prevTotalFlexes, newTotalFlexes);
        flexes[column] += prevTotalFlexes - newTotalFlexes;
        prefs[column] = basePrefs[column];
        return newFlexWidth;
    }

    public static boolean greater(double a, double b) {
        return a - b > 0.001;
    }

    private static boolean greaterEquals(double a, double b) {
        return a - b > -0.001;
    }

    public static boolean equals(double a, double b) {
        return Math.abs(a - b) < 0.001;
    }

    // this algorithm assumes that all prefs are known and can be changed
    // prefs на double'ах чтобы не "дрожало", из-за преобразований в разные стороны (строго говоря наверное без adjustTableFixed и overflow дрожать не будет)
    // viewfixed if view is fixed we can convert flex to pref, otherwise we can't
    public static double calculateNewFlexes(int column, double delta, int viewWidth, double[] prefs, double[] flexes, int[] basePrefs, double[] baseFlexes, boolean viewFixed) {
        boolean removeLeftPref = true; // вообще так как removeLeftFlex false, логично иметь симметричное поведение, но больше не меньше (removeRightPref и add*Pref не имеют смысла, так как вся delta просто идет в pref колонки)

        // ищем первую динамическую компоненту слева (она должна получить +delta, соответственно правая часть -delta)
        // тут есть варианты -delta идет одной правой колонке, или всем правых колонок, но так как
        // a) так как выравнивание по умолчанию левое, интуитивно при перемещении изменяют именно размер левой колонки, б) так как есть де-факто ограничение Preferred, вероятность получить нужный размер уменьшая все колонки куда выше
        // будем распределять между всеми правыми колонками

        // находим левую flex
        while (column >= 0 && baseFlexes[column] == 0)
            column--;
        if (column < 0) // нет левой flex колонки - ничего не делаем
            return delta;

        // считаем общий текущий preferred
        double totalPref = 0;
        for (double pref : prefs) {
            totalPref += pref;
        }

        // сначала списываем delta справа налево pref (но не меньше basePref), ПОКА сумма pref > viewWidth !!! ( то есть flex не работает, работает ширина контейнера или minTableWidth в таблице)
        // тут можно было бы если идет расширение - delta > 0.0, viewWidth приравнять totalPref (соответственно запретить adjust, то есть pref'ы остались такими же) и reduce'ить остальные, но это пойдет в разрез с уменьшением (когда нужно уменьшать pref'ы иначе в исходное состояние не вернешься), поэтому логичнее исходить из концепции когда если есть scroll тогда просто расширяем колонки, если нет scroll'а пытаемся уместить все без скролла
        double exceedPrefWidth = totalPref - viewWidth;
        if (greaterEquals(exceedPrefWidth, 0.0)) {
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

        if (equals(delta, 0.0)) // все расписали
            return delta;

        double flexWidth = -exceedPrefWidth;
        assert greaterEquals(flexWidth, 0.0);

        // можно переходить на basePref - flex (с учетом того что viewWidth может измениться, pref'ы могут быть как равны viewWidth в результате предыдущего шага, так и меньше)
        for (int i = 0; i < prefs.length; i++)
            flexWidth = reducePrefsToBase(flexWidth, i, prefs, flexes, basePrefs);

        //если flexWidth все еще равно 0 - вываливаемся (так как нельзя меньше preferred опускаться)
        if (equals(flexWidth, 0.0))
            return delta; // or maybe 0.0

        // запускаем изменение flex'а (пропорциональное)
        double totalFlex = 0;
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
        }

        // flex колонки увеличиваем на нужную величину, соответственно остальные flex'ы надо уменьшить на эту величину
        double toAddFlex = delta * totalFlex / flexWidth;

        double shrinkedFlex = 0.0;
        if (greater(0.0, toAddFlex + flexes[column])) { // не shrink'аем, но и левые столбцы не уменьшаются (то есть removeLeftFlex false)
            shrinkedFlex = toAddFlex + flexes[column];
            toAddFlex = -flexes[column];
        }

        double restFlex = 0.0; // flex that wasn't added to the right flexes
        double toAddRightFlex = toAddFlex;
        if(equals(totalRightBaseFlexes, 0.0)) { // if there are no right flex columns, we don't change flexes
            restFlex = toAddRightFlex;
        } else {
            if (toAddRightFlex > totalRightFlexes) { // we don't want to have negative flexes
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
        }

        flexes[column] += toAddFlex - restFlex;

        // если и так осталась, то придется давать preferred (соответственно flex не имеет смысла) и "здравствуй" scroll
        if (!equals(restFlex, 0.0) && viewFixed) {
            // we can't increase / decrease right part using flexes (we're out of it they are zero already, since restflex is not zero), so we have to use prefs instead
            // assert that right flexes are zero (so moving flex width to prefs in left part won't change anything)
            for (int i = 0; i <= column; i++)
                prefs[i] += flexWidth * flexes[i] / totalFlex;
            prefs[column] += flexWidth * restFlex / totalFlex;
            restFlex = 0.0;
        }
        return (restFlex + shrinkedFlex) * flexWidth / totalFlex;
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

    //  prefs parameter is filled
    public static double calculateNewFlexesForFixedTableLayout(int column, int delta, int viewWidth, double[] prefs, int[] basePrefs, boolean[] flexes) {
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

        double restDelta = calculateNewFlexes(column, delta, viewWidth, prefs, flexValues, basePrefs, baseFlexValues, true);

        adjustFlexesToFixedTableLayout(viewWidth, prefs, flexes, flexValues);

        return restDelta;
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

    public static String editParse(String value) {
        String groupingSeparator = LocaleInfo.getCurrentLocale().getNumberConstants().groupingSeparator();
        if (UNBREAKABLE_SPACE.equals(groupingSeparator)) {
            value = value.replace(" ", UNBREAKABLE_SPACE);
        }
        String decimalSeparator = LocaleInfo.getCurrentLocale().getNumberConstants().decimalSeparator();
        return replaceSeparators(value, decimalSeparator, groupingSeparator);
    }

    public static String editFormat(String value) {
        String groupingSeparator = LocaleInfo.getCurrentLocale().getNumberConstants().groupingSeparator();
        value = value.replace(groupingSeparator, "");
        //need because of IntegralCellEditor.createInputElement
        if(MainFrame.mobile)
            value = GwtClientUtils.replaceCommaSeparator(value);
        return value;
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

    public static native int getHeight(Element element) /*-{
        return parseInt($wnd.getComputedStyle(element, null).height);
    }-*/;

    public static native int getWidth(Element element) /*-{
        return parseInt($wnd.getComputedStyle(element, null).width);
    }-*/;

    public static native int getMarginTop(Element element) /*-{
        return parseInt($wnd.getComputedStyle(element, null).marginTop);
    }-*/;

    public static native int getAllMargins(Element element) /*-{
        var computedStyle = $wnd.getComputedStyle(element, null);
        return parseInt(computedStyle.marginTop) + parseInt(computedStyle.marginBottom) +
                parseInt(computedStyle.borderTop) + parseInt(computedStyle.borderBottom) +
                parseInt(computedStyle.paddingTop) + parseInt(computedStyle.paddingBottom);
    }-*/;

    public static native int getFullHeight(Element element) /*-{
        var computedStyle = $wnd.getComputedStyle(element, null);
        return element.offsetHeight + parseInt(computedStyle.marginTop) + parseInt(computedStyle.marginBottom);
    }-*/;

    public static native int getFullWidth(Element element) /*-{
        var computedStyle = $wnd.getComputedStyle(element, null);
        return element.offsetWidth + parseInt(computedStyle.marginLeft) + parseInt(computedStyle.marginRight);
    }-*/;

    public static native void setProperty(Style style, String property, String value, String priority) /*-{
        style.setProperty(property, value, priority);
    }-*/;

    public static native Element getFocusedElement() /*-{
        return $doc.activeElement;
    }-*/;

    public static native Element log(String i) /*-{
        console.log(i);
    }-*/;

    public static Element getFocusedChild(Element containerElement) {
        Element focusedElement = getFocusedElement();
        if(containerElement.isOrHasChild(focusedElement))
            return focusedElement;
        return null;
    }

    public static boolean isTDorTH(Element element) {
        String tagName = element.getTagName().toLowerCase();
        return tagName.equals("td") || tagName.equals("th");
    }


    public static <T> T findInList(List<T> list, Predicate<T> predicate) {
        for(T element : list)
            if(predicate.test(element))
                return element;
        return null;
    }

    public static <E extends Exception> Object parseInterval(String s, EFunction<String, Long, E> parseFunction) throws E {
        String[] dates = s.split(" - ");
        Long epochFrom = parseFunction.apply(dates[0]);
        Long epochTo = parseFunction.apply(dates[1]);
        return epochFrom <= epochTo ? new BigDecimal(epochFrom + "." + epochTo) : null;
    }

    public static String formatInterval(Object obj, Function<Long, String> formatFunction) {
        return formatFunction.apply(getIntervalPart(obj, true)) + " - " + formatFunction.apply(getIntervalPart(obj, false));
    }

    public static Long getIntervalPart(Object o, boolean from) {
        String object = String.valueOf(o);
        int indexOfDecimal = object.indexOf(".");
        return Long.parseLong(indexOfDecimal < 0 ? object : from ? object.substring(0, indexOfDecimal) : object.substring(indexOfDecimal + 1));
    }

    public static Element wrapCenteredImg(Element th, Integer height, Consumer<ImageElement> imgProcessor) {
        th = wrapDiv(th); // we need to wrap in div, since we don't want to modify th itself (it's not recreated every time for grid) + setting display flex for th breaks layouting + for th it's unclear how to make it clip text that doesn't fit height (even max-height)

        // since it's a header we want to align it to the center (vertically and horizontally)
        th = wrapCenter(th); // we have to do it after setting height (because that's the point of that centering)
        // left works strange, plus, images are also stretched, so will leave it with extra container
//        setAlignedFlexCenter(th, multiLine ? "stretch" : "center", multiLine ? "left" : "center"); // in theory should correspond default alignments in TextBasedCellRenderer

        // we don't want that container to be larger than the upper one
        // it seems it is needed because in wrapDiv we use auto sizing
        if(height != null)
            th.getStyle().setProperty("maxHeight", height + "px");

        if(imgProcessor != null)
            th = wrapImg(th, imgProcessor);
//            th = wrapAlignedFlexImg(th, imgProcessor);

        th.addClassName("wrap-caption");
        return th;
    }

    public static void clearAlignedFlexCenter(Element th) {
        th.removeClassName("wrap-center");
        th.getStyle().clearProperty("alignItems");
        th.getStyle().clearProperty("justifyContent");
    }
    // optimization
    public static boolean isAlignedFlexModifiableDiv(Element th) {
        return th.hasClassName("wrap-center");
    }

    //  will wrap with div, because otherwise other wrappers will add and not remove classes after update
    public static Element wrapDiv(Element th) {
        Element wrappedTh = Document.get().createDivElement();
        wrappedTh.addClassName("wrap-div");
        th.appendChild(wrappedTh);

        return wrappedTh;
    }

    public static Element wrapCenter(Element th) {
        th.addClassName("wrap-center"); // display flex : justify-content, align-items : center

        Element wrappedTh = Document.get().createDivElement();
        th.appendChild(wrappedTh);

        return wrappedTh;
    }

    public static Element wrapImg(Element th, Consumer<ImageElement> imgProcessor) {
        assert !isAlignedFlexModifiableDiv(th);
        th.addClassName("wrap-wrapimgdiv");

        Element wrappedTh = Document.get().createDivElement();
        wrappedTh.addClassName("wrap-imgdiv");

        ImageElement img = Document.get().createImageElement();
        img.addClassName("wrap-img-margins");
        img.addClassName("wrap-img");
        imgProcessor.accept(img);
        th.appendChild(img);

        th.appendChild(wrappedTh);

        return wrappedTh;
    }

    public static Element wrapAlignedFlexImg(Element th, Consumer<ImageElement> imgProcessor) {
        assert isAlignedFlexModifiableDiv(th) || isTDorTH(th); // has vertical and text align

        Element wrappedTh = Document.get().createDivElement();

        ImageElement img = Document.get().createImageElement();
        img.addClassName("wrap-img-margins");
        imgProcessor.accept(img);
        th.appendChild(img);

        th.appendChild(wrappedTh);

        return wrappedTh;
    }

    public static boolean nullEquals(Object obj1, Object obj2) {
        if (obj1 == null)
            return obj2 == null;
        if (obj1 == obj2)
            return true;
        return obj1.equals(obj2);
    }

    public static int nullHash(Object obj) {
        return obj == null ? 0 : obj.hashCode();
    }

    private static ArrayList emptyList = new ArrayList();
    public static <K> ArrayList<K> EMPTYARRAYLIST() {
        return emptyList;
    }

    public static boolean hashEquals(Object obj1, Object obj2) {
        return obj1 == obj2 || (obj1.hashCode() == obj2.hashCode() && obj1.equals(obj2));
    }

    public static boolean nullHashEquals(Object obj1, Object obj2) {
        if (obj1 == null)
            return obj2 == null;
        return obj2 != null && hashEquals(obj1, obj2);
    }

    public static <T> ArrayList<T> newArrayList(T[] array) {
        ArrayList<T> list = new ArrayList<T>(array.length);
        for(T element : array)
            list.add(element);
        return list;
    }

    public static Element getElement(Node node) {
        if(node == null)
            return null;
        if(Element.is(node))
            return Element.as(node);
        return node.getParentElement();
    }

    // adding two exclusive orderered sets
    public static <K> void addOrderedSets(ArrayList<K> listTo, ArrayList<? extends K> listFrom) {
        if(listTo.isEmpty()) { // optimization
            listTo.addAll(listFrom);
            return;
        }
        for(K element : listFrom)
            if(!listTo.contains(element))
                listTo.add(element);
    }

    public static <T> T nvl(T value1, T value2) {
        return value1 == null ? value2 : value1;
    }

    public static Element getParentWithAttribute(Element element, String property) {
        while (element != null) {
            if (!element.getAttribute(property).isEmpty()) {
                return element;
            }
            element = element.getParentElement();
        }
        return null;
    }
    public static Element getParentWithClass(Element element, String className) {
        while (element != null) {
            if (element.hasClassName(className)) {
                return element;
            }
            element = element.getParentElement();
        }
        return null;
    }

    public static <T> ArrayList<T> removeLast(ArrayList<T> values) {
        ArrayList<T> newValues = new ArrayList<>();
        for (int i = 0; i < values.size() - 1; i++)
            newValues.add(values.get(i));
        return newValues;
    }
    public static <T> T[] add(T[] array1, T[] array2, Function<Integer, T[]> instancer) {
        T[] result = instancer.apply(array1.length + array2.length);
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    //when used in gwt-javascript, so as not to pass many parameters to the native-method and get localized strings directly
    protected static native void getLocalizedString(String string)/*-{
        var name;
        var prototype = Object.getPrototypeOf(@GwtClientUtils::messages);
        var ownPropertyNames = Object.getOwnPropertyNames(prototype);
        for (var i = 0; i < ownPropertyNames.length; i++) {
            var property = ownPropertyNames[i];
            if (property.includes(string)) {
                name = property;
                break;
            }
        }
        return name != null ? prototype[name]() : name;
    }-*/;

    public static String getFileExtension(String filename) {
        int index = filename.lastIndexOf(".");
        return (index == -1) ? "" : filename.substring(index + 1);
    }

    // need this because views like leaflet and some others uses z-indexes and therefore dialogs for example are shown below layers,
    public static void setZeroZIndex(Element element) {
        element.getStyle().setZIndex(0);
    }

    public static native JavaScriptObject getGlobalField(String field)/*-{
        var jsField = $wnd[field];
        if (jsField != null)
            return jsField;
        else
            throw new Error("Field " + field + " not found");
    }-*/;
    public static native JavaScriptObject getField(JavaScriptObject object, String field)/*-{
        return object[field];
    }-*/;
    public static native JavaScriptObject call(JavaScriptObject object)/*-{
        return object();
    }-*/;
    public static native JavaScriptObject call(JavaScriptObject object, Object param)/*-{
        return object(param);
    }-*/;
    public static native JavaScriptObject call(JavaScriptObject object, JsArray<JavaScriptObject> params)/*-{
        return object.apply(object, params);
    }-*/;
    public static native JavaScriptObject newObject()/*-{
        return {};
    }-*/;
    public static native void setField(JavaScriptObject object, String field, JavaScriptObject value)/*-{
        return object[field] = value;
    }-*/;

    public static JsDate toJsDate(Date date) {
        if(date == null)
            return null;
        return JsDate.create(date.getTime());
    }
    public static Date fromJsDate(JsDate date) {
        if(date == null)
            return null;
        return new Date(Math.round(date.getTime()));
    }
    public static native JsDate getUTCDate(int year, int month, int date, int hours, int minutes, int seconds)/*-{
        return new Date(Date.UTC(year, month, date, hours, minutes, seconds));
    }-*/;


    public static native JavaScriptObject sortArray(JavaScriptObject array, String sortField, boolean reverseOrder)/*-{
        return array.sort(function (a, b) {
            return reverseOrder ? b[sortField] - a[sortField] : a[sortField] - b[sortField];
        });
    }-*/;

    public static native boolean isJSObjectPropertiesEquals(JavaScriptObject object1, JavaScriptObject object2)/*-{
        var keys = Object.keys(object1);
        for (var i = 0; i < keys.length; i++) {
            if (!keys[i].startsWith('#') && object1[keys[i]] !== object2[keys[i]])
                return false;
        }
        return true;
    }-*/;

    public static native boolean isFunctionContainsArguments(JavaScriptObject fn)/*-{
        var str = fn.toString().replace(/\/\*[\s\S]*?\*\//g, '')
            .replace(/\/\/(.)*\\/g, '')
            .replace(/{[\s\S]*}/, '')
            .replace(/=>/g, '')
            .trim();

        return str.substring(str.indexOf("(") + 1, str.length - 1).length > 0;
    }-*/;

    public static native JavaScriptObject jsonParse(String value)/*-{
        try {
            if(value == null)
                return null;
            return JSON.parse(value);
        } catch(e) {
            return {};
        }
    }-*/;

    public static native String jsonStringify(JavaScriptObject value)/*-{
        try {
            if(value == null)
                return null;
            return JSON.stringify(value);
        } catch(e) {
            return "{}";
        }
    }-*/;

    public static native void consoleError(String error)/*-{
        console.error(error);
    }-*/;

    public static native void setOnMouseDown(Element element, Consumer<NativeEvent> run)/*-{
        element.onmousedown = function(event) {
            run.@Consumer::accept(*)(event);
        }
    }-*/;
}
