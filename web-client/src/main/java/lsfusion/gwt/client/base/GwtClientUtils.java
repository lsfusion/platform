package lsfusion.gwt.client.base;

import com.google.gwt.core.client.*;
import com.google.gwt.dom.client.*;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.lambda.EFunction;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.PopupOwner;
import lsfusion.gwt.client.base.view.popup.PopupPanel;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.view.MainFrame;

import java.util.*;
import java.util.function.*;

import static java.lang.Math.max;
import static lsfusion.gwt.client.base.GwtSharedUtils.isRedundantString;
import static lsfusion.gwt.client.base.GwtSharedUtils.replicate;
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

    // GWT utility methods
    public native static void init() /*-{
        $wnd.lsfUtils = {
            setFocusElement: function (element, focusElement) {
                return @lsfusion.gwt.client.form.property.cell.view.CellRenderer::setFocusElement(*)(element, focusElement);
            },
            addDropDownPartner: function (element, partner) {
                return @lsfusion.gwt.client.base.GwtClientUtils::addDropDownPartner(*)(element, partner);
            },
            setOnFocusOut: function (element, onFocusOut) {
                return @lsfusion.gwt.client.base.FocusUtils::setOnFocusOutFnc(*)(element, onFocusOut);
            },
            setOnFocusOutWithDropDownPartner: function (element, partner, onFocusOut) {
                return @lsfusion.gwt.client.base.FocusUtils::setOnFocusOutWithDropDownPartner(*)(element, partner, onFocusOut);
            },
            removeOnFocusOut: function (element) {
                return @lsfusion.gwt.client.base.FocusUtils::removeOnFocusOut(*)(element);
            },
            isSuppressOnFocusChange: function (element) {
                return @lsfusion.gwt.client.base.FocusUtils::isSuppressOnFocusChange(*)(element);
            },
            clearFocusElement: function (element) {
                return @lsfusion.gwt.client.form.property.cell.view.CellRenderer::clearFocusElement(*)(element);
            },
            setReadonlyFnc: function (element, readonlyFnc) {
                return @lsfusion.gwt.client.form.property.cell.view.CellRenderer::setReadonlyFnc(*)(element, readonlyFnc);
            },
            clearReadonlyFnc: function (element) {
                return @lsfusion.gwt.client.form.property.cell.view.CellRenderer::clearReadonlyFnc(*)(element);
            },
            useBootstrap: function() {
                return @lsfusion.gwt.client.view.MainFrame::useBootstrap;
            },
            isTDorTH: function(element) {
                return @lsfusion.gwt.client.base.GwtClientUtils::isTDorTH(*)(element);
            }
        }
    }-*/;

    public static InputElement createInputElement(String type) {
        Element input;
        if(type.equals("textarea")) {
            input = createFocusElement("textarea");
        } else {
            input = createFocusElement("input");
            input.setAttribute("type", type);
        }
        return (InputElement) input;
    };

    public native static Element createFocusElement(String tag) /*-{
        return $wnd.createFocusElement(tag);
    }-*/;
    public native static Element createFocusElementType(String tag) /*-{
        return $wnd.createFocusElementType(tag);
    }-*/;

    public static void logout() {
        logout(false);
    }

    public static void logout(boolean connectionLost) {
        MainFrame.disableConfirmDialog = true;
        //CloseNavigator should be called before logout, because logout removes authentication
        MainFrame.cleanRemote(() -> Window.open(GwtClientUtils.getLogoutUrl(), "_self", null), connectionLost);
    }

    public static void openFile(String name, boolean autoPrint, Integer autoPrintTimeout) {
        if (name != null) {
            JavaScriptObject window = openWindow(getAppDownloadURL(name));
            if (autoPrint)
                print(window, autoPrintTimeout);
        }
    }

    public static void downloadFile(String fileUrl) {
        if (fileUrl != null)
            fileDownload(getAppDownloadURL(fileUrl));
    }

    public static native JavaScriptObject openWindow(String url)/*-{
        return $wnd.open(url);
    }-*/;

    //js does not allow to download files. use the solution from https://stackoverflow.com/questions/3916191/download-data-url-file
    public static native void fileDownload(String url)/*-{
        var document = $wnd.document;
        var downloadLink = document.createElement("a");
        downloadLink.setAttribute("download", "");
        downloadLink.href = url;
        document.body.appendChild(downloadLink);
        downloadLink.click();
        document.body.removeChild(downloadLink);
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

    private static String getURL(String url) {
        return url == null ? null : getWebAppBaseURL() + url;
    }

    // the one that is in gwt(main)/public/static/images
    // FileUtils.STATIC_IMAGE_FOLDER_PATH
    public static String getStaticImageURL(String imagePath) {
        return getURL(imagePath != null ? MainFrame.staticImagesURL + imagePath : null); // myapp/imagepath
//        return imagePath == null ? null : GWT.getModuleBaseURL() + "static/images/" + imagePath; // myapp/main/static/images/myimage/imagepath
    }
    // the on that is in the app server resources
    public static String getAppStaticImageURL(String imagePath) {
        assert imagePath != null;
        return getURL(imagePath);
    }

    public static String getAppStaticWebURL(String filePath) {
        return getURL(filePath);
    }
    // FileUtils.APP_DOWNLOAD_FOLDER_PATH
    public static String getAppDownloadURL(String url) {
        assert url != null;
        return getURL(url);
    }

    // FileUtils.APP_UPLOAD_FOLDER_PATH
    public static String getUploadURL(String fileName) {
        return getURL("uploadFile" + (fileName != null ? "?sid=" + fileName : ""));
    }

    public static final String LSF_CLASSES_ATTRIBUTE = "lsf_classes_attribute";

    public static void setThemeImage(String imagePath, Consumer<String> modifier) {
        assert imagePath != null;
        modifier.accept(getThemeImage(imagePath));
    }

    public static String getThemeImage(String imagePath) {
        return getStaticImageURL(!colorTheme.isDefault() ? colorTheme.getImagePath(imagePath) : imagePath);
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

    public static void addClassNames(Element element, String classNames) {
        element.addClassName(classNames);
    }
    public static void removeClassNames(Element element, String classNames) {
        element.removeClassName(classNames);
    }

    public static String removeClassName(String classNames, String removeClassName, Result<Boolean> removed) {
        removed.set(false);

        String result = classNames;
        if(classNames != null && classNames.contains(removeClassName)) { // optimization
            String[] aClassNames = classNames.split(" ");
            result = "";
            for(int i = 0; i <aClassNames.length ;i++) {
                String className = aClassNames[i];
                if(className.equals(removeClassName))
                    removed.set(true);
                else
                    result += (i != 0 ? " " : "") + className;
            }
        }
        return result;
    }

    public static boolean hasClassNamePrefix(String classNames, String classNamePrefix) {
        if(classNames != null && classNames.contains(classNamePrefix)) { // optimization
            String[] aClassNames = classNames.split(" ");
            for(int i = 0; i <aClassNames.length ;i++) {
                String className = aClassNames[i];
                if(className.startsWith(classNamePrefix))
                    return true;
            }
        }
        return false;
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

    public static final native TableSectionElement createTBody(TableElement element) /*-{
        return element.createTBody();
    }-*/;

    public static boolean isString(JavaScriptObject object, String string) {
        return object != null && object.toString().equals(string);
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
        return createHorizontalSeparator().toString();
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

    public static boolean isFirefoxUserAgent() {
        String userAgent = getUserAgent();
        return userAgent.contains("firefox");
    }

    public static boolean isShowing(Widget widget) {
        if (widget == null) {
            return false;
        }
        return isShowing(widget.getElement());
    }
    public static boolean isShowing(Element el) {
        if (el == null) {
            return false;
        }
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
        child.addClassName("fill-parent-absolute");
    }

    private static void setupFillParentElement(Element parentElement) {
        String parentPosition = parentElement.getStyle().getPosition();
        if (parentPosition == null || parentPosition.isEmpty() || parentPosition.equals(Style.Position.STATIC.getCssName()))
            parentElement.addClassName("fill-parent-position");
    }

    public static void clearFillParent(Element child) {
        clearFillParentElement(child.getParentElement());
        child.removeClassName("fill-parent-absolute");
    }

    public static void clearFillParentElement(Element parentElement) {
        String parentPosition = parentElement.getStyle().getPosition();
        if (parentPosition != null && parentPosition.equals(Style.Position.RELATIVE.getCssName()))
            parentElement.removeClassName("fill-parent-position");
    }

    public static void setupFlexParentElement(Element parentElement) {
        assert !GwtClientUtils.isTDorTH(parentElement);
        parentElement.addClassName("fill-parent-flex-cont");
    }

    public static void clearFlexParentElement(Element parentElement) {
        assert !GwtClientUtils.isTDorTH(parentElement);
        parentElement.removeClassName("fill-parent-flex-cont");
    }

    public static void setupFlexParent(Element element) {
        setupFlexParentElement(element.getParentElement());

        element.addClassName("fill-parent-flex");
    }

    public static void setupPercentParent(Element element) {
        element.addClassName("fill-parent-perc");
//        element.getStyle().setWidth(100, Style.Unit.PCT);
//        element.getStyle().setHeight(100, Style.Unit.PCT);
////        inputElement.addClassName("boxSized");
//        element.getStyle().setProperty("boxSizing", "border-box");
    }

    public static void clearPercentParent(Element element) {
        element.removeClassName("fill-parent-perc");
//        element.getStyle().clearWidth();
//        element.getStyle().clearHeight();
//        element.getStyle().clearProperty("boxSizing");
    }

    /*--- tippy methods ---*/

    public static JavaScriptObject showTippyPopup(Widget widget, Widget popupWidget) {
        return showTippyPopup(new PopupOwner(widget), popupWidget);
    }

    public static JavaScriptObject showTippyPopup(PopupOwner popupOwner, Widget popupWidget) {
        return showTippyPopup(popupOwner, popupWidget, null);
    }

    public static JavaScriptObject showTippyPopup(PopupOwner popupOwner, Widget popupWidget, Runnable onHideAction) {
        RootPanel.get().add(popupWidget);
        return showTippyPopup(popupOwner, popupWidget.getElement(), onHideAction);
    }

    public static JavaScriptObject showTippyPopup(PopupOwner popupOwner, Element popupElement, Runnable onHideAction) {
        return showTippyPopup(popupOwner, popupElement, onHideAction, null);
    }

    public static JavaScriptObject showTippyPopup(PopupOwner popupOwner, Element popupElement, Runnable onHideAction, Supplier<Element> referenceElementSupplier) {
        JavaScriptObject tippy = initTippyPopup(popupOwner, popupElement, "manual", onHideAction, null, referenceElementSupplier);
        showTippy(tippy);
        return tippy;
    }

    public static JavaScriptObject initTippyPopup(PopupOwner popupOwner, Element popupElement, String target, Runnable onHideAction, Runnable onShowAction, Supplier<Element> referenceElementSupplier) {
        JavaScriptObject tippy = initTippy(popupOwner, 0, target, onHideAction, onShowAction, referenceElementSupplier);
        updateTippyContent(tippy, popupElement);
        return tippy;
    }

    // the partner with other inner lsf components ("recursive" partner)
    public static void addPopupPartner(PopupOwner owner, Element popup) {
        popup.addClassName("popup-partner");
        FocusUtils.addFocusPartner(owner.element, popup);
    }
    //  the "deadend" partner
    // focus problems are handled by edit mechanism or custom elements themselves
    public static void addDropDownPartner(Element owner, Element dropdown) {
        dropdown.addClassName("dropdown-partner");
        FocusUtils.addFocusPartner(owner, dropdown);
    }
    public static void removePopupPartner(PopupOwner owner, Element popup, boolean blurred) {
        if(!blurred)
            FocusUtils.focusInOut(owner.element, FocusUtils.Reason.RESTOREFOCUS);
    }

    public static JavaScriptObject initTippy(PopupOwner popupOwner, int delay, String trigger, Runnable onHideAction, Runnable onShowAction, Supplier<Element> referenceElementSupplier) {
        assert popupOwner.element != null;
        JavaScriptObject tippy = initTippy(popupOwner.element, delay, trigger,
        (instance) -> {
            removePopupPartner(popupOwner, getPopup(instance), blurredTippy);
            if(onHideAction != null && !silentTippy)
                onHideAction.run();
        },
        (instance) -> {
            addPopupPartner(popupOwner, getPopup(instance));
            if(onShowAction != null && !silentTippy)
                onShowAction.run();
        }, referenceElementSupplier);
        Widget ownerWidget = popupOwner.widget;
        if(ownerWidget != null) {
            ownerWidget.addAttachHandler(attachEvent -> {
                if(!attachEvent.isAttached()) {
                    GwtClientUtils.hideAndDestroyTippyPopup(tippy, true);
                }
            });
        }
        return tippy;
    }

    public static void hideAndDestroyTippyPopup(JavaScriptObject popup) {
        hideAndDestroyTippyPopup(popup, false);
    }
    private static boolean silentTippy;
    private static boolean blurredTippy;
    public static void hideTippy(JavaScriptObject popup, boolean silent, boolean blurred) {
        boolean prevSilentTippy = silentTippy;
        boolean prevBlurredTippy = blurredTippy;
        silentTippy = silent;
        blurredTippy = blurred;
        try {
            hideTippy(popup);
        } finally {
            silentTippy = prevSilentTippy;
            blurredTippy = prevBlurredTippy;
        }
    };

    public static native void updateTippyContent(JavaScriptObject tippy, Element content)/*-{
        tippy.setContent(content);
        if(content == null)
            tippy.disable();
        else
            tippy.enable();
    }-*/;

    public static native void showTippy(JavaScriptObject tippy)/*-{
        tippy.show();
    }-*/;
    public static void hideAndDestroyTippyPopup(JavaScriptObject popup, boolean silent) {
        hideTippy(popup, silent, false);
        destroyTippy(popup);
    };
    private static native void hideTippy(JavaScriptObject tippy)/*-{
        tippy.hide();
    }-*/;
    public static native void destroyTippy(JavaScriptObject tippy)/*-{
        tippy.destroy();
    }-*/;
    public static native Element getPopup(JavaScriptObject tippy)/*-{
        return tippy.popper;
    }-*/;
    public static void setHideOnBlur(JavaScriptObject tippy) {
        FocusUtils.setOnFocusOut(getPopup(tippy), nativeEvent -> hideTippy(tippy, false, true));
    }

    private static native JavaScriptObject initTippy(Element element, int delay, String trigger, Consumer<JavaScriptObject> onHideAction, Consumer<JavaScriptObject> onShowAction, Supplier<Element> referenceElementSupplier)/*-{
        return $wnd.tippy(element, {
            appendTo: function() {
                return $doc.body;
            },
            //content: contentElement,
            trigger: trigger,
            interactive: true,
            allowHTML: true,
            placement: 'auto',
            maxWidth: 'none', // default maxWidth is 350px and content does not fit in tooltip
            popperOptions: {
                strategy: 'fixed',
                modifiers: [
                    {
                        name: 'flip',
                        options: {
                            fallbackPlacements: ['top', 'bottom', 'left', 'right'],
                        },
                    },
                    {
                        name: 'preventOverflow',
                        options: {
                            altAxis: true,
                            tether: false,
                        },
                    },
                ],
            },
            getReferenceClientRect: function() {
                var referenceElement = null;
                // changing reference element to the inner one if needed (this.contextElement.getBoundingClientRect() seems to be the default implementation)
                if(referenceElementSupplier != null)
                    referenceElement = referenceElementSupplier.@Supplier::get()();
                if(referenceElement == null)
                    referenceElement = this.contextElement;
                return referenceElement.getBoundingClientRect();
            },
            silent: false,

            // we're changing document mousedown scheme to the focus / blur (as in all the other places)
            hideOnClick: false,
            onCreate: function(instance) { // copy of hideOnPopperBlur (with a isFakeBlur emulation)
                @GwtClientUtils::setHideOnBlur(*)(instance);
            },
            onShown: function(instance) {
                instance.popper.firstChild.focus(); // we need to set focus inside to make focusOut work
            },

            onShow: function(instance) {
                onShowAction.@java.util.function.Consumer::accept(*)(instance);
            },
            onHide: function(instance) { // we need on hide to have focus on tooltip and thus be able to move it before it goes to the body
                onHideAction.@java.util.function.Consumer::accept(*)(instance);
            },
            delay: [delay, null]
        });
    }-*/;

    /*--- end of tippy methods ---*/

    public static void setPopupPosition(PopupPanel popup, int mouseX, int mouseY) {
        int popupWidth = popup.getOffsetWidth();
        int popupHeight = popup.getOffsetHeight();
        int xCorrection = popupWidth - (Window.getClientWidth() - mouseX);
        int yCorrection = popupHeight - (Window.getClientHeight() - mouseY);

        if (xCorrection > 0 || yCorrection > 0) {
            if (xCorrection > 0 && yCorrection > 0) {
                // For the same reason with a lack of space on both sides (right and bottom) we show popup on the opposite side of the cursor.
                // Otherwise, in Firefox we won't see the popup at all.
                popup.setPopupPosition(max(mouseX - popupWidth, 0), max(mouseY - popupHeight, 0));
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

    public static GSize getOffsetWidth(Element element) {
        final int width = element.getOffsetWidth();

        return GSize.getOffsetSize((int) Math.round((double) width));
    }
    public static GSize getOffsetHeight(Element element) {
        final int height = element.getOffsetHeight();

        return GSize.getOffsetSize((int) Math.round((double) height));
    }

    public static GSize getClientWidth(Element element) {
        final int width = element.getClientWidth();

        return GSize.getOffsetSize((int) Math.round((double) width));
    }
    public static GSize getClientHeight(Element element) {
        final int height = element.getClientHeight();

        return GSize.getOffsetSize((int) Math.round((double) height));
    }
    public static native double getDoubleOffsetWidth(Element element) /*-{
        return element.getBoundingClientRect().width;
    }-*/;
    public static native double getDoubleOffsetLeft(Element element) /*-{
        return element.getBoundingClientRect().left;
    }-*/;


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
    public static double calculateNewFlexes(int column, double delta, int viewWidth, double[] prefs, double[] flexes, int[] basePrefs, double[] baseFlexes, boolean[] flexPrefs, boolean noParentFlex, Boolean resizeOverflow, int margins, boolean wrap) {
        // ищем первую динамическую компоненту слева (она должна получить +delta, соответственно правая часть -delta)
        // тут есть варианты -delta идет одной правой колонке, или всем правых колонок, но так как
        // a) так как выравнивание по умолчанию левое, интуитивно при перемещении изменяют именно размер левой колонки, б) так как есть де-факто ограничение Preferred, вероятность получить нужный размер уменьшая все колонки куда выше
        // будем распределять между всеми правыми колонками

        // находим левую flex
        int flexColumn = column;
        while (flexColumn >= 0 && baseFlexes[flexColumn] == 0)
            flexColumn--;

        // считаем общий текущий preferred
        double totalPref = 0;
        for (double pref : prefs) {
            totalPref += pref;
        }

        if (flexColumn < 0) {
            double restDelta = 0.0;
            if(resizeOverflow != null && !resizeOverflow) { // we shouldn't exceed viewWidth, but only if it is set explicitly (otherwise behaviour is pretty normal)
                double restWidth = viewWidth - totalPref - margins;
                if(delta > restWidth) {
                    restDelta = delta - restWidth;
                    delta = restWidth;
                }
            }

            return restDelta + (-reducePrefs(-delta, column, prefs, basePrefs, flexPrefs));
        }
        column = flexColumn;

        // сначала списываем delta справа налево pref (но не меньше basePref), ПОКА сумма pref > viewWidth !!! ( то есть flex не работает, работает ширина контейнера или minTableWidth в таблице)
        // тут можно было бы если идет расширение - delta > 0.0, viewWidth приравнять totalPref (соответственно запретить adjust, то есть pref'ы остались такими же) и reduce'ить остальные, но это пойдет в разрез с уменьшением (когда нужно уменьшать pref'ы иначе в исходное состояние не вернешься), поэтому логичнее исходить из концепции когда если есть scroll тогда просто расширяем колонки, если нет scroll'а пытаемся уместить все без скролла
        double exceedPrefWidth = totalPref - viewWidth;
        if (greaterEquals(exceedPrefWidth, 0.0)) {
            double prefReduceDelta = Math.min(-delta, exceedPrefWidth);
            delta += prefReduceDelta;
            reducePrefs(prefReduceDelta, column, prefs, basePrefs, null);

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
        if (!equals(restFlex, 0.0) && noParentFlex && (resizeOverflow != null ? resizeOverflow : !wrap)) {
            // we can't increase / decrease right part using flexes (we're out of it they are zero already, since restflex is not zero), so we have to use prefs instead
            // assert that right flexes are zero (so moving flex width to prefs in left part won't change anything)
            for (int i = 0; i <= column; i++)
                prefs[i] += flexWidth * flexes[i] / totalFlex;
            prefs[column] += flexWidth * restFlex / totalFlex;
            restFlex = 0.0;
        }

        if(equals(totalFlex, 0.0)) {
            assert equals(restFlex + shrinkedFlex, 0.0);
            return 0.0;
        }

        return (restFlex + shrinkedFlex) * flexWidth / totalFlex;
    }

    private static double reducePrefs(double delta, int column, double[] prefs, int[] basePrefs, boolean[] filterColumns) {
        for (int i = column; i >= 0; i--) {
            if(filterColumns == null || filterColumns[i]) {
                double maxReduce = prefs[i] - basePrefs[i];
                double reduce = Math.min(delta, maxReduce);
                prefs[i] -= reduce;
                delta -= reduce;
                if (equals(delta, 0.0))
                    break;
            }
        }
        return delta;
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

    public static String getCurrentLocaleName() {
        return LocaleInfo.getCurrentLocale().getLocaleName();
    } 
    
    public static String getCurrentLanguage() {
        return getCurrentLocaleName().substring(0, 2);
    }

    public static boolean hasVerticalScroll(Element element) {
        return element.getScrollHeight() > element.getClientHeight();
    }

    public static native int getColumnGap(Element element) /*-{
        return parseInt($wnd.getComputedStyle(element, null).columnGap) || 0; // can be 'normal' and others
    }-*/;

    public static int getScrollWidth(Element element) {
        return element.getOffsetWidth() - element.getClientWidth() - getBorderWidth(element); // in theory borders should be excluded, but for now it doesn't matter
    }

    public static int getScrollHeight(Element element) {
        return element.getOffsetHeight() - element.getClientHeight() - getBorderHeight(element); // in theory borders should be excluded, but for now it doesn't matter
    }

    // without padding, getClient - with paddings, getOffset with paddings and borders (+scrolls), getFull wit paddings, borders and margins
    public static native int getHeight(Element element) /*-{
        return parseInt($wnd.getComputedStyle(element, null).height);
    }-*/;

    public static native int getWidth(Element element) /*-{
        return parseInt($wnd.getComputedStyle(element, null).width);
    }-*/;

    public static native int getMarginTop(Element element) /*-{
        return parseInt($wnd.getComputedStyle(element, null).marginTop);
    }-*/;

    public static native int getBorderHeight(Element element) /*-{
        var computedStyle = $wnd.getComputedStyle(element, null);
        return parseInt(computedStyle.borderTopWidth) + parseInt(computedStyle.borderBottomWidth);
    }-*/;

    public static native int getBorderWidth(Element element) /*-{
        var computedStyle = $wnd.getComputedStyle(element, null);
        return parseInt(computedStyle.borderLeftWidth) + parseInt(computedStyle.borderRightWidth);
    }-*/;

    public static native int getDoubleBorderRightWidth(Element element) /*-{
        var computedStyle = $wnd.getComputedStyle(element, null);
        return parseFloat(computedStyle.borderRightWidth);
    }-*/;

    public static native double getDoubleBorderLeftWidth(Element element) /*-{
        var computedStyle = $wnd.getComputedStyle(element, null);
        return parseFloat(computedStyle.borderLeftWidth);
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

    public static JsArrayString toArray(List<String> list) {
        JsArrayString jsArray = JsArrayString.createArray().cast();
        list.forEach(jsArray::push);
        return jsArray;
    }
    public static List<String> fromArray(JsArrayString array) {
        List<String> list = new ArrayList<>();
        for (int i = 0, size = array.length(); i < size; i++)
            list.add(array.get(i));
        return list;
    }

    public static native Element log(String i) /*-{
        console.log(i);
    }-*/;

    public static boolean isTDorTH(Element element) {
        return TableCellElement.is(element);
    }
    public static boolean isInput(Element element) {
        return InputElement.is(element) || TextAreaElement.is(element);
    }

    public static <T> T findInList(List<T> list, Predicate<T> predicate) {
        for(T element : list)
            if(predicate.test(element))
                return element;
        return null;
    }

    public static <E extends Exception> PValue parseInterval(String s, EFunction<String, Long, E> parseFunction) throws E {
        String[] dates = s.split(" - ");
        Long epochFrom = parseFunction.apply(dates[0]);
        Long epochTo = parseFunction.apply(dates[1]);
        return epochFrom <= epochTo ? PValue.getPValue(epochFrom, epochTo) : null;
    }

    public static String getStep(int precision) {
        if(precision == 0)
            return "1";
        return "0." + replicate('0', precision <= 5 ? precision - 1 : 4) + "1";
    }

    public static String formatInterval(PValue obj, Function<Long, String> formatFunction) {
        return formatInterval(formatFunction.apply(PValue.getIntervalValue(obj, true)), formatFunction.apply(PValue.getIntervalValue(obj, false)));
    }

    public static String formatInterval(String left, String right) {
        return left + " - " + right;
    }

    //  will wrap with div, because otherwise other wrappers will add and not remove classes after update
    public static Element wrapDiv(Element th) {
        Element wrappedTh = Document.get().createDivElement();
        wrappedTh.addClassName("wrap-div");
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

    public static native boolean isConnected(Element element)/*-{
        return element.isConnected;
    }-*/;

    public static Element getParentWithNonEmptyAttribute(Element element, String property) {
        while (element != null) {
            if (!element.getAttribute(property).isEmpty()) {
                return element;
            }
            element = element.getParentElement();
        }
        return null;
    }
    public static Element getParentWithAttribute(Element element, String property) {
        while (element != null) {
            if (element.hasAttribute(property)) {
                return element;
            }
            element = element.getParentElement();
        }
        return null;
    }
    public static Element getParentWithProperty(Element element, String property) {
        while (element != null) {
            if (element.getPropertyObject(property) != null) {
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
    public static native JavaScriptObject call(JavaScriptObject object, JavaScriptObject param)/*-{
        return object(param);
    }-*/;
    public static native JavaScriptObject call(JavaScriptObject object, JavaScriptObject param1, JavaScriptObject param2)/*-{
        return object(param1, param2);
    }-*/;
    public static native JavaScriptObject call(JavaScriptObject object, JsArray<JavaScriptObject> params)/*-{
        return object.apply(object, params);
    }-*/;
    public static native JavaScriptObject newObject()/*-{
        return {};
    }-*/;
    public static native JsArray emptyArray()/*-{
        return [];
    }-*/;
    public static native void setField(JavaScriptObject object, String field, JavaScriptObject value)/*-{
        return object[field] = value;
    }-*/;

    public static native JavaScriptObject replaceField(JavaScriptObject object, String field, JavaScriptObject value)/*-{
        return $wnd.replaceField(object, field, value);
    }-*/;

    public static native JavaScriptObject removeField(JavaScriptObject object, String field)/*-{
        if (object[field]) {
            value = object[field];
            delete object[field];
            return value;
        }
        return null;
    }-*/;

    public static native JavaScriptObject toJsObject(String field1, JavaScriptObject value1) /*-{
        var obj = $wnd.createPlainObject();
        obj[field1] = value1;
        return obj;
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
    public static native int getUTCYear(JsDate date)/*-{
        return date.getUTCFullYear();
    }-*/;
    public static native int getUTCMonth(JsDate date)/*-{
        return date.getUTCMonth();
    }-*/;
    public static native int getUTCDate(JsDate date)/*-{
        return date.getUTCDate();
    }-*/;
    public static native int getUTCHours(JsDate date)/*-{
        return date.getUTCHours();
    }-*/;
    public static native int getUTCMinutes(JsDate date)/*-{
        return date.getUTCMinutes();
    }-*/;
    public static native int getUTCSeconds(JsDate date)/*-{
        return date.getUTCSeconds();
    }-*/;


    public static native JavaScriptObject sortArray(JavaScriptObject array, String sortField, boolean reverseOrder)/*-{
        return array.sort(function (a, b) {
            return reverseOrder ? b[sortField] - a[sortField] : a[sortField] - b[sortField];
        });
    }-*/;

    public static native int javaScriptObjectHashCode(JavaScriptObject object)/*-{
        var hash = 0;
        var str = JSON.stringify(object);
        for (var j = 0, len = str.length; j < len; j++) {
            hash = (hash << 5) - hash + str.charCodeAt(j);
            hash |= 0; // Convert to 32bit integer
        }
        return hash;
    }-*/;

    public static native int javaScriptObjectAllFieldsHashCode(JavaScriptObject object)/*-{
        var keys = Object.keys(object).filter(function (objectKey) { return !objectKey.startsWith('#') });
        var hash = 0;
        for (var i = 0; i < keys.length; i++) {
            var key = keys[i];
            hash += @GwtClientUtils::javaScriptObjectHashCode(*)(key) ^ @GwtClientUtils::javaScriptObjectHashCode(*)(object[key]);
        }
        return hash;
    }-*/;

    public static native boolean plainEquals(JavaScriptObject object1, JavaScriptObject object2, String ignoreField)/*-{
        return $wnd.plainEquals(object1, object2, ignoreField);
    }-*/;

    public static native void registerServiceWorker(Consumer<JavaScriptObject> onMessage, JavaScriptObject message)/*-{
        $wnd.registerServiceWorker(function (message) {
            onMessage.@java.util.function.Consumer::accept(*)(message);
        }, message);
    }-*/;

    public static native String subscribePushManager(String publicKey, Consumer<String> onSubscribe)/*-{
        $wnd.subscribePushManager(publicKey, function (subscription) {
            onSubscribe.@java.util.function.Consumer::accept(*)(subscription);
        });
    }-*/;

    public static native String unsubscribePushManager()/*-{
        $wnd.unsubscribePushManager();
    }-*/;

    public static native void openBroadcastChannel(String channelName, BiConsumer<JavaScriptObject, String> onMessage)/*-{
        $wnd.openBroadcastChannel(channelName, function (broadcastChannel, message) {
            onMessage.@java.util.function.BiConsumer::accept(*)(broadcastChannel, message);
        });
    }-*/;
    public static native void postBroadcastChannelMessage(JavaScriptObject channel, String message)/*-{
        $wnd.postBroadcastChannelMessage(channel, message);
    }-*/;

    public static native void requestPushNotificationPermissions()/*-{
        $wnd.requestPushNotificationPermissions();
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

    public static native void fireOnMouseDown(Element element)/*-{
        element.dispatchEvent(new MouseEvent("mousedown"));
    }-*/;

    public static native void setOnMouseDown(Element element, Consumer<NativeEvent> run)/*-{
        element.onmousedown = function(event) {
            run.@Consumer::accept(*)(event);
        }
    }-*/;

    public static native void setOnClick(Element element, Consumer<NativeEvent> run)/*-{
        element.onclick = function(event) {
            run.@Consumer::accept(*)(event);
        }
    }-*/;

    // should be used only once for elements that don't have widgets
    public static void setEventListener(Element element, int eventID, com.google.gwt.user.client.EventListener listener) {
        DOM.sinkEvents(element, eventID);
        assert DOM.getEventListener(element) == null;
        DOM.setEventListener(element, listener);
    }

    private static String showIfVisible = "showIfVisible";
    public static void setShowIfVisible(Widget widget, boolean visible) {
        widget.getElement().setAttribute(showIfVisible, String.valueOf(visible));
        updateVisibility(widget);
    }

    private static String gridVisible = "gridVisible";
    public static void setGridVisible(Widget widget, boolean visible) {
        widget.getElement().setAttribute(gridVisible, String.valueOf(visible));
        updateVisibility(widget);
    }

    private static void updateVisibility(Widget widget) {
        widget.setVisible(isVisible(widget, showIfVisible) && isVisible(widget, gridVisible));
    }

    private static boolean isVisible(Widget widget, String key) {
        String value = widget.getElement().getAttribute(key);
        return isRedundantString(value) || Boolean.parseBoolean(value);
    }

    public static String escapeSeparator(String value, GCompare compare) {
        if (value != null) {
            if (compare.escapeSeparator())
                value = value.replace(MainFrame.matchSearchSeparator, "\\" + MainFrame.matchSearchSeparator);
            if (compare == GCompare.CONTAINS)
                value = value.replace("%", "\\%").replace("_", "\\_");
        }
        return value;
    }

    public static String getEventCaption(String keyEventCaption, String mouseEventCaption) {
        return keyEventCaption != null ? (mouseEventCaption != null ? (keyEventCaption + " / " + mouseEventCaption) : keyEventCaption) : mouseEventCaption;
    }

    public static native void resizable(Element element, String handles, Consumer<NativeEvent> handler)/*-{
        $wnd.$(element).resizable({
            handles: handles,
            resize: function (event, ui) { handler.@Consumer::accept(*)(event); }
        });
    }-*/;

    public static native void draggable(Element element, String handle)/*-{
        $wnd.$(element).draggable({ handle: handle});
    }-*/;

    public static final native NodeList<Element> getElementsByClassName(String className) /*-{
        return $doc.getElementsByClassName(className);
    }-*/;

    public static native boolean containsLineBreak(String value) /*-{
        return $wnd.containsLineBreak(value);
    }-*/;
    public static native boolean containsHtmlTag(String value) /*-{
        return $wnd.containsHtmlTag(value) != null;
    }-*/;


    public static void initCaptionHtmlOrText(Element element, CaptionHtmlOrTextType type) {
        initCaptionHtmlOrText(element, type.getCssClass());
    }
    public static void initDataHtmlOrText(Element element, DataHtmlOrTextType type) {
        initDataHtmlOrText(element, type.getCssClass());
    }
    private static native void initCaptionHtmlOrText(Element element, String cssClass) /*-{
        $wnd.initCaptionHtmlOrText(element, cssClass);
    }-*/;
    public static native void initDataHtmlOrText(Element element, String cssClass) /*-{
        $wnd.initDataHtmlOrText(element, cssClass);
    }-*/;
    // elements used in this set method should be created with initCaptionHtmlOrText
    public static native boolean setCaptionHtmlOrText(Element element, String value) /*-{
        $wnd.setCaptionHtmlOrText(element, value);
    }-*/;
    public static native boolean setCaptionNodeText(Node node, String value) /*-{
        $wnd.setCaptionNodeText(node, value);
    }-*/;
    public static native void setDataHtmlOrText(Element element, String value, boolean html) /*-{
        $wnd.setDataHtmlOrText(element, value, html);
    }-*/;
    public static void clearDataHtmlOrText(Element element, DataHtmlOrTextType type) {
        clearDataHtmlOrText(element, type.getCssClass());
    }
    private static native void clearDataHtmlOrText(Element element, String cssClass) /*-{
        $wnd.clearDataHtmlOrText(element, cssClass);
    }-*/;

    public static native void setMask(Element element, JavaScriptObject options)/*-{
        $wnd.$(element).inputmask(options);
    }-*/;

    public static native void removeMask(Element element)/*-{
        $wnd.$(element).inputmask("remove");
    }-*/;

    public static native boolean isCompleteMask(Element element)/*-{
        return $wnd.$(element).inputmask("isComplete");
    }-*/;
    
    public static native String unmaskedValue(Element element)/*-{
        return $wnd.$(element).inputmask("unmaskedvalue");
    }-*/;

    public static native boolean hasProperty(JavaScriptObject object, String property)/*-{
        return !!object[property];
    }-*/;
}
