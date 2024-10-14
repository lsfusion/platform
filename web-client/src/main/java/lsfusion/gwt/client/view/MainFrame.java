package lsfusion.gwt.client.view;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.*;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.GNavigatorChangesDTO;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.base.exception.GExceptionManager;
import lsfusion.gwt.client.base.log.GLog;
import lsfusion.gwt.client.base.result.VoidResult;
import lsfusion.gwt.client.base.view.DialogBoxHelper;
import lsfusion.gwt.client.base.view.PopupOwner;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.controller.dispatch.LogicsDispatchAsync;
import lsfusion.gwt.client.controller.remote.GConnectionLostManager;
import lsfusion.gwt.client.controller.remote.action.PriorityAsyncCallback;
import lsfusion.gwt.client.controller.remote.action.PriorityErrorHandlingCallback;
import lsfusion.gwt.client.controller.remote.action.RequestAsyncCallback;
import lsfusion.gwt.client.controller.remote.action.RequestCountingAsyncCallback;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.navigator.*;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.event.GMouseStroke;
import lsfusion.gwt.client.form.object.table.grid.user.design.GColorPreferences;
import lsfusion.gwt.client.form.object.table.grid.view.GSimpleStateTableView;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.classes.controller.DateRangePickerBasedCellEditor;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.navigator.controller.GNavigatorController;
import lsfusion.gwt.client.navigator.controller.dispatch.GNavigatorActionDispatcher;
import lsfusion.gwt.client.navigator.controller.dispatch.NavigatorDispatchAsync;
import lsfusion.gwt.client.navigator.view.BSMobileNavigatorView;
import lsfusion.gwt.client.navigator.view.ExcelMobileNavigatorView;
import lsfusion.gwt.client.navigator.view.MobileNavigatorView;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;
import lsfusion.gwt.client.navigator.window.view.WindowsController;
import net.customware.gwt.dispatch.shared.Result;

import java.util.*;

import static lsfusion.gwt.client.base.BaseImage.BASE_STATIC_IMAGE;

// scope - every single tab (not browser) even for static
public class MainFrame implements EntryPoint {
    public static LogicsDispatchAsync logicsDispatchAsync;
    public static NavigatorDispatchAsync navigatorDispatchAsync;

    public static boolean mobile;
    public static MobileNavigatorView mobileNavigatorView = null;
    public static int mobileAdjustment;

    public static boolean firefox;
    public static boolean chrome;

    // settings    
    public static boolean devMode;
    public static String projectLSFDir;
    public static boolean showDetailedInfo;
    public static boolean autoReconnectOnConnectionLost;
    public static int showDetailedInfoDelay;
    public static boolean suppressOnFocusChange;
    public static boolean forbidDuplicateForms;
    public static boolean useBootstrap;
    public static String size;
    public static long busyDialogTimeout;
    public static long updateRendererStateSetTimeout = 100;
    public static boolean pivotOnlySelectedColumn;
    public static String matchSearchSeparator;
    private static Boolean shouldRepeatPingRequest = true;
    public static boolean disableConfirmDialog = false;
    public static String staticImagesURL;
    
    public static GColorTheme colorTheme = GColorTheme.DEFAULT;
    public static Map<String, String> versionedColorThemesCss;
    public static List<ColorThemeChangeListener> colorThemeChangeListeners = new ArrayList<>();
    
    public static GColorPreferences colorPreferences;

    public static boolean hasCapitalHyphensProblem; // first capital is not hyphenized

    public static String dateFormat;
    public static String timeFormat;
    public static String dateTimeFormat;
    public static String[] preDefinedDateRangesNames;

    public static boolean useTextAsFilterSeparator;

    public static boolean verticalNavbar;

    public static boolean userFiltersManualApplyMode;

    public static boolean disableActionsIfReadonly;
    public static boolean enableShowingRecentlyLogMessages;
    public static String pushNotificationPublicKey;
    
    public static double maxStickyLeft;

    public static boolean jasperReportsIgnorePageMargins;

    public static double v5 = 5.9999;
    public static double cssBackwardCompatibilityLevel;

    // async dispatch
    public <T extends Result> long asyncDispatch(final ExecuteNavigatorAction action, RequestCountingAsyncCallback<ServerResponseResult> callback) {
        return navigatorDispatchAsync.asyncExecute(action, callback);
    }

    public <T extends Result> long syncDispatch(final NavigatorRequestAction<T> action, RequestCountingAsyncCallback<T> callback) {
        return syncDispatch(action, callback, false);
    }

    public static <T extends Result> long syncDispatch(final NavigatorRequestAction<T> action, RequestAsyncCallback<T> callback, boolean continueInvocation) {
        return navigatorDispatchAsync.syncExecute(action, callback, continueInvocation);
    }

    public static void cleanRemote(Runnable runnable, boolean connectionLost) {
        if (navigatorDispatchAsync != null && !connectionLost) { // dispatcher may be not initialized yet (at first look up logics call)
            navigatorDispatchAsync.executePriority(new CloseNavigator(), new PriorityAsyncCallback<VoidResult>() {
                public void onFailure(Throwable caught) {
                    runnable.run();
                }

                public void onSuccess(VoidResult result) {
                    runnable.run();
                }
            });
        } else {
            runnable.run();
        }
    }

    public void onModuleLoad() {
        GwtClientUtils.init();

        firefox = GwtClientUtils.isFirefoxUserAgent();
        chrome = GwtClientUtils.isChromeUserAgent();
        if (chrome)
            GwtClientUtils.setGlobalClassName(true, "is-chrome");

        hackForGwtDnd();

        RootPanel.getBodyElement().setTabIndex(-1); // we need this because activeElement returns body (under the spec) when there is no active element, and we sometimes need to return focus there
        GwtClientUtils.addClassName(RootLayoutPanel.get().getElement(), "root-layout-panel");
//        GwtClientUtils.setZeroZIndex(element); // ??? move to layout.css

        PopupOwner popupOwner = PopupOwner.GLOBAL; // actually now is used for error handling

        GWT.setUncaughtExceptionHandler(t -> {
            if (!ignoreException(t)) {
                GExceptionManager.logClientError(t, popupOwner);
                DialogBoxHelper.showMessageBox("Error", t.getMessage(), popupOwner, null);
            }
        });

        initializeLogicsAndNavigator(popupOwner);
    }

    private static boolean ignoreException(Throwable exception) {
        String message = exception.getMessage();
        //ace.js has unusual behaviour, and for unknown reasons periodically gives an Uncaught NetworkError about not being able to load a worker which is already loaded
        //in gwt 2.10.0 we receive this error as 'null' (for unknown reasons real message is lost in Impl.java
        if(message != null && message.equals("null"))
            return true;

        /*
            When a dialogue window is opened, the window is resizable.
            If you start resizing window on touch screens and move your finger very quickly and randomly for a long time, an error is thrown from
            com.allen_sauer.gwt.dnd.client.onTouchEndorCancel(TouchEvent<?> event) {
                if (event.getTouches().length() != 0) {
                    /.../
                }
                /.../
            }
            because at some time the "event.getTouches()" becomes null, and we get "Cannot read properties of null (reading 'length')".

            Because we want to remove current resize logic in the future, now we try suppressing this error
         */
        String stackTrace = Arrays.toString(exception.getStackTrace());
        if (stackTrace.contains("com.allen_sauer.gwt.dnd.client.MouseDragHandler") && stackTrace.contains("onTouchEndorCancel") && stackTrace.contains("com.allen_sauer.gwt.dnd.client.MouseDragHandler.onTouchEnd"))
            return true;

        return false;
    }
    
    private static class Linker<T> {
        private T link;
    }

    public static boolean switchedToAnotherWindow;

    public static boolean previewSwitchToAnotherWindow(Event event) {
        if (DataGrid.FOCUSPREVIEWIN.equals(event.getType())) {
            if (switchedToAnotherWindow) {
                switchedToAnotherWindow = false;
                return true;
            }
        } else if (DataGrid.FOCUSPREVIEWOUT.equals(event.getType())) {
            switchedToAnotherWindow = isSwitchedToAnotherWindow(event, Document.get());
            return switchedToAnotherWindow;
        }
        return false;
    }

    //heuristic
    //'visibilitychange' will not work, because 'focus' event is caught by editor earlier then by whole document
    //(https://stackoverflow.com/questions/28993157/visibilitychange-event-is-not-triggered-when-switching-program-window-with-altt)
    //ignore focus/blur events when switching tabs/windows:
    //https://stackoverflow.com/questions/61713458/ignore-blur-focusout-events-when-switching-tabs-windows
    private static native boolean isSwitchedToAnotherWindow(Event event, Document document) /*-{
        return event.target === document.activeElement;
    }-*/;

    // it's odd, but dblclk works even when the first click was on different target
    // for example double clicking on focused property, causes first mousedown/click on that property, after that its handler dialog is shown
    // the second click is handled by dialog, and double click is also triggered for that dialog (that shouldn't happen given to the browser dblclick specification), causing it to be hidden
    // so this way we fix that browser bug
    private static Element beforeLastClickedTarget;
    private static Element lastClickedTarget;
    private static Event lastClickedEvent;
    private static Event lastUpEvent;
    private static Event lastDownEvent;
    public static boolean previewClickEvent(Element target, Event event) {
        if (GMouseStroke.isClickEvent(event)) {
            if (event != lastClickedEvent) { // checking lastClickedEvent since it can be propagated (or not)
                lastClickedEvent = event;
                beforeLastClickedTarget = lastClickedTarget;
                lastClickedTarget = target;

                if(ignoreClickAfterDown(target, true)) {
                    GwtClientUtils.stopPropagation(event);
                    return false;
                }
            }
        }
        if (GMouseStroke.isUpEvent(event)) {
            if (event != lastUpEvent) { // checking lastClickedEvent since it can be propagated (or not)
                lastUpEvent = event;

                if(ignoreClickAfterDown(target, false)) {
                    GwtClientUtils.stopPropagation(event);
                    return false;
                }
            }
        }
        if (GMouseStroke.isDownEvent(event)) {
            if (event != lastDownEvent) { // checking lastDownEvent since it can be propagated (or not)
                lastDownEvent = event;

                ignoreClickAfterDown(target, true); // just in case if we missed click event
            }
        }
        if(GMouseStroke.isDblClickEvent(event)) {
            if(lastClickedTarget != null && ignoreDblClickAfterClick(lastClickedTarget))
                return false;
            if(beforeLastClickedTarget != null && lastClickedTarget != null && target == lastClickedTarget && beforeLastClickedTarget != lastClickedTarget && noIgnoreDblClickCheck(lastClickedTarget))
                return false;
        }
        return true;
    }

    //if we process two single clicks, we don't want to process double click
    public static final String IGNORE_DBLCLICK_AFTER_CLICK = "__ignore_dblclick_after_click";

    public static void preventDblClickAfterClick(Element element) {
        element.setAttribute(IGNORE_DBLCLICK_AFTER_CLICK, "true");

        new Timer() {
            @Override
            public void run() {
                element.removeAttribute(IGNORE_DBLCLICK_AFTER_CLICK);
            }
        }.schedule(500);
    }

    private static boolean ignoreDblClickAfterClick(Element element) {
        return GwtClientUtils.getParentWithAttribute(element, IGNORE_DBLCLICK_AFTER_CLICK) != null;
    }

    //we change element at first click and should process dblclick
    public static final String IGNORE_DBLCLICK_CHECK = "__ignore_dblclick_check";

    //lastClickedTarget and beforeLastClickedTarget can be not equal if we change element at first click
    private static boolean noIgnoreDblClickCheck(Element element) {
        return GwtClientUtils.getParentWithAttribute(element, IGNORE_DBLCLICK_CHECK) == null;
    }

    //if we process two single clicks, we don't want to process double click
    public static final String IGNORE_CLICK_AFTER_DOWN = "__ignore_click_after_down";

    public static void preventClickAfterDown(Element element, Event event) {
        element.setAttribute(IGNORE_CLICK_AFTER_DOWN, "true");
        lastDownEvent = event;
    }

    private static boolean ignoreClickAfterDown(Element element, boolean click) {
        boolean hasAttribute = element.hasAttribute(IGNORE_CLICK_AFTER_DOWN);
        if(click) // click always goes after up
            element.removeAttribute(IGNORE_CLICK_AFTER_DOWN);
        return hasAttribute;
    }

    public void initializeFrame(NavigatorInfo result, PopupOwner popupOwner) {
        assert currentForm == null;
        currentForm = null;

        final Linker<GAbstractWindow> formsWindowLink = new Linker<>();
        final Linker<Map<GAbstractWindow, Widget>> commonWindowsLink = new Linker<>();
        final Linker<GNavigatorController> navigatorControllerLink = new Linker<>();
        final Linker<FormsController> formsControllerLinker = new Linker<>();
        final WindowsController windowsController = new WindowsController() {
            @Override
            public Widget getWindowView(GAbstractWindow window) {
                Widget view;
                if (window.equals(formsWindowLink.link)) {
                    view = formsControllerLinker.link.getView();
                } else if (window instanceof GNavigatorWindow) {
                    view = navigatorControllerLink.link.getNavigatorWidgetView((GNavigatorWindow) window);
                } else {
                    view = commonWindowsLink.link.get(window);
                }
                return view;
            }
        };

        final Linker<GNavigatorActionDispatcher> actionDispatcherLink = new Linker<>();
        final FormsController formsController = new FormsController(windowsController) {
            @Override
            public <T extends Result> long syncDispatch(NavigatorRequestAction<T> action, RequestCountingAsyncCallback<T> callback) {
                return MainFrame.this.syncDispatch(action, callback);
            }

            @Override
            public long asyncDispatch(ExecuteNavigatorAction action, RequestCountingAsyncCallback<ServerResponseResult> callback) {
                return MainFrame.this.asyncDispatch(action, callback);
            }

            @Override
            public GNavigatorActionDispatcher getDispatcher() {
                return actionDispatcherLink.link;
            }
        };

        formsControllerLinker.link = formsController;

        //we use CloseHandler instead of Window.ClosingHandler because mobile browsers send closing event without closing window
//        if(mobile) {
            Window.addCloseHandler(event -> {
                saveAndClean(windowsController);
            });
//        } else { // somewhy in browser close handler doesn't work
        // !!! but closing handler can be called too early
//            Window.addWindowClosingHandler(event -> {
//                saveAndClean(windowsController);
//            });
//        }

        Window.addWindowClosingHandler(event -> {
            if(!disableConfirmDialog) {
                event.setMessage("confirm message"); //message is ignored, browsers show default messages
            }
        });

        GNavigatorController navigatorController = new GNavigatorController(formsController) {
            @Override
            public void updateVisibility(Map<GAbstractWindow, Boolean> windows) {
                windowsController.updateVisibility(windows);
            }
        };
        navigatorControllerLink.link = navigatorController;

        actionDispatcherLink.link = new GNavigatorActionDispatcher(windowsController, formsController, navigatorController);

        initializeWindows(result, formsController, windowsController, navigatorController, formsWindowLink, commonWindowsLink);

        GConnectionLostManager.start();

        Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                if (shouldRepeatPingRequest && !GConnectionLostManager.shouldBeBlocked()) {
                    setShouldRepeatPingRequest(false);
                    navigatorDispatchAsync.executePriority(new ClientPushMessage(), new PriorityErrorHandlingCallback<ClientMessageResult>(popupOwner) {
                        @Override
                        public void onSuccess(ClientMessageResult result) {
                            setShouldRepeatPingRequest(true);
                            for (Integer idNotification : result.notificationList)
                                formsController.executeNotificationAction(idNotification, null, null);
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            setShouldRepeatPingRequest(true);
                            super.onFailure(caught);
                        }
                    });
                }
                return true;
            }
        }, 1000);

        GExceptionManager.flushUnreportedThrowables(popupOwner);
    }

    private void saveAndClean(WindowsController windowsController) {
        try {
            if (!mobile) {
                windowsController.storeWindowsSizes();
            }
            windowsController.storeEditMode();
        } finally {
            clean();
        }
    }

    public static void setShouldRepeatPingRequest(boolean ishouldRepeatPingRequest) {
        shouldRepeatPingRequest = ishouldRepeatPingRequest;
    }
    
    private boolean isShouldRepeatPingRequest() {
        return shouldRepeatPingRequest;
    }
    
    public static void addColorThemeChangeListener(ColorThemeChangeListener listener) {
        colorThemeChangeListeners.add(listener);
    }
    
    public static void changeColorTheme(GColorTheme newColorTheme) {
        if (colorTheme != newColorTheme) {
            colorTheme = newColorTheme;

            Element cssLink = Document.get().getElementById("themeCss");
            String colorThemeSid = colorTheme.getSid();
            cssLink.setAttribute("href", versionedColorThemesCss.get(colorThemeSid));

            Document.get().getDocumentElement().setAttribute("data-bs-theme", colorThemeSid);

            StyleDefaults.reset();

            NodeList<Element> imgTextElements = GwtClientUtils.getElementsByClassName("img-text-widget");
            for(int i = 0, size = imgTextElements.getLength(); i < size; i++) {
                Element imgTextElement = imgTextElements.getItem(i);
                BaseStaticImage baseImage = (BaseStaticImage) imgTextElement.getPropertyObject(BASE_STATIC_IMAGE);
                if(baseImage != null) // if it uses themed image
                    BaseImage.updateImage(baseImage, imgTextElement);
            }

            for (ColorThemeChangeListener colorThemeChangeListener : colorThemeChangeListeners) {
                colorThemeChangeListener.colorThemeChanged();
            }
        }
    } 

    private void hackForGwtDnd() {
        RootPanel.get().getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);

        //gwt-dnd ориентируется на body.clientHeight для ограничения области перетаскивания
        //поэтому приходится в явную проставлять размеры у <body>
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                updateBodyDimensions();
            }
        });
        Window.addResizeHandler(new ResizeHandler() {
            public void onResize(ResizeEvent event) {
                updateBodyDimensions();
            }
        });

//        DebugHelper.install();
    }

    private void updateBodyDimensions() {
        Style bodyStyle = RootPanel.get().getElement().getStyle();
        bodyStyle.setHeight(Window.getClientHeight(), Style.Unit.PX);
        bodyStyle.setWidth(Window.getClientWidth(), Style.Unit.PX);
    }

    private static FormContainer currentForm;
    private static boolean modalPopup;

    public static void setCurrentForm(FormContainer currentForm) {
        MainFrame.currentForm = currentForm;
    }

    public static void setModalPopup(boolean modalPopup) {
        MainFrame.modalPopup = modalPopup;
    }

    public static FormContainer getCurrentForm() {
        if(!modalPopup)
            return currentForm;
        return null;
    }
    public static FormContainer getAssertCurrentForm() {
//        assert !modalPopup; // this assertion can be broken in tooltips (since their showing is async) - for example it's showing is scheduled, change initiated, after that tooltip is showm and then response is received and message is shown
        return currentForm;
    }
    public static boolean isModalPopup() {
        return modalPopup;
    }

    private void initializeWindows(NavigatorInfo result, final FormsController formsController, final WindowsController windowsController, final GNavigatorController navigatorController, final Linker<GAbstractWindow> formsWindowLink, final Linker<Map<GAbstractWindow, Widget>> commonWindowsLink) {
        GwtClientUtils.removeLoaderFromHostedPage();

        GAbstractWindow formsWindow = result.forms;
        formsWindowLink.link = formsWindow;
        Map<GAbstractWindow, Widget> commonWindows = new LinkedHashMap<>();
        commonWindows.put(result.log, GLog.createLogPanel(result.log.visible));
        commonWindowsLink.link = commonWindows;

        ArrayList<GNavigatorWindow> navigatorWindows = result.navigatorWindows;

        navigatorController.setRoot(result.root);

        GwtClientUtils.setGlobalClassName(true, useBootstrap ? "nav-bootstrap" : "nav-excel");
        GwtClientUtils.setGlobalClassName(true, mobile ? "nav-mobile" : "nav-desktop");
        GwtClientUtils.setGlobalClassName(true, "size-" + size);
//        FormsController.setGlobalClassName(contentWordWrap, "content-word-wrap");

        if (mobile) {
            if (useBootstrap) {
                mobileNavigatorView = new BSMobileNavigatorView(navigatorWindows, windowsController, navigatorController);
            } else {
                mobileNavigatorView = new ExcelMobileNavigatorView(navigatorWindows, windowsController, navigatorController);
            }

            windowsController.registerMobileWindow(formsWindow);

            RootLayoutPanel.get().add(windowsController.getWindowView(formsWindow));
        } else {
            if (MainFrame.verticalNavbar) {
                // change navbar navigators orientation
                for (GAbstractWindow window : result.navigatorWindows) {
                    if (window instanceof GNavigatorWindow) {
                        GNavigatorWindow toolbarWindow = (GNavigatorWindow) window;
                        if (toolbarWindow.isInRootNavBar()) {
                            toolbarWindow.vertical = true;
                            toolbarWindow.verticalTextPosition = GNavigatorWindow.CENTER;
                        }
                    }
                }
            }

            navigatorController.initializeNavigatorViews(navigatorWindows);

            List<GAbstractWindow> allWindows = new ArrayList<>();
            allWindows.addAll(navigatorWindows);
            allWindows.addAll(commonWindows.keySet());

            windowsController.initializeWindows(allWindows, formsWindow);
        }

        formsController.initRoot();

        //apply initial navigator changes from navigatorinfo somewhere around here
        applyNavigatorChanges(result.navigatorChanges, navigatorController, windowsController);

        formsController.executeAction("SystemEvents.onClientStartedApply[]", () -> {
            if (formsController.getFormsCount() == 0) {
                openNavigatorMenu();
            }
        });

        GwtClientUtils.registerServiceWorker(message -> {
            String type = GSimpleStateTableView.toString(GwtClientUtils.getField(message, "type"));
            if(type.equals("pushNotification")) {
                int notificationId = GSimpleStateTableView.toInt(GwtClientUtils.getField(message, "id"));
                String notificationResult = GSimpleStateTableView.toString(GwtClientUtils.getField(message, "result"));
                formsController.executeNotificationAction(notificationId, notificationResult, null);
            } else if(type.equals("clientId")) {
                updateServiceClientInfo(formsController, null, GSimpleStateTableView.toString(GwtClientUtils.getField(message, "clientId")));
            }
        }, GwtClientUtils.toJsObject("type", GSimpleStateTableView.fromString("pullNotification")));

        GwtClientUtils.requestPushNotificationPermissions();

        GwtClientUtils.subscribePushManager(pushNotificationPublicKey, subscription -> updateServiceClientInfo(formsController, subscription, null));
    }

    public static String subscription;
    public static String clientId;
    private void updateServiceClientInfo(FormsController formsController, String subscription, String clientId) {
        if(subscription != null)
            MainFrame.subscription = subscription;
        if(clientId != null)
            MainFrame.clientId = clientId;
        if(MainFrame.clientId != null && MainFrame.subscription != null)
            formsController.executeSystemAction(new UpdateServiceClientInfoAction(MainFrame.subscription, MainFrame.clientId));
    }

    public static void applyNavigatorChanges(GNavigatorChangesDTO navigatorChangesDTO, GNavigatorController navigatorController, WindowsController windowsController) {
        if (navigatorChangesDTO.properties.length == 0) { // optimization
            return;
        }

        for(int i = 0; i < navigatorChangesDTO.properties.length; i++) {
            navigatorChangesDTO.properties[i].update(navigatorController, windowsController, PValue.convertFileValue(navigatorChangesDTO.values[i]));
        }

        // here we do not do incremental update, but global "refresh" (as we use the same mechanism for selected mechanism)
        if (!mobile) {
            navigatorController.update();
        }
    }

    private native String getSessionId() /*-{
        return $wnd.document.body.getAttribute("sessionID");
    }-*/;

    public void initializeLogicsAndNavigator(PopupOwner popupOwner) {
        String portString = Window.Location.getParameter("port");
        Integer screenWidth = Window.getClientWidth();
        Integer screenHeight = Window.getClientHeight();
        double scale = getScale();
        mobile = Math.min(screenHeight, screenWidth) <= StyleDefaults.maxMobileWidthHeight;
        mobileAdjustment = mobile ? 1 : 0;

        logicsDispatchAsync = new LogicsDispatchAsync(Window.Location.getParameter("host"), portString != null ? Integer.valueOf(portString) : null,
                Window.Location.getParameter("exportName"));

        navigatorDispatchAsync = new NavigatorDispatchAsync(getSessionId());
        navigatorDispatchAsync.executePriority(new InitializeNavigator(screenWidth + "x" + screenHeight, scale, mobile), new PriorityErrorHandlingCallback<InitializeNavigatorResult>(popupOwner) {
            @Override
            public void onSuccess(InitializeNavigatorResult result) {
                GClientSettings gClientSettings = result.gClientSettings;

                versionedColorThemesCss = gClientSettings.versionedColorThemesCss;
                busyDialogTimeout = Math.max(gClientSettings.busyDialogTimeout - 500, 500); // minimum timeout 500ms + there is still a delay of about 500ms
                staticImagesURL = gClientSettings.staticImagesURL;
                useBootstrap = gClientSettings.useBootstrap;
                size = gClientSettings.size;
                devMode = gClientSettings.devMode;
                projectLSFDir = gClientSettings.projectLSFDir;
                showDetailedInfo = gClientSettings.showDetailedInfo;
                showDetailedInfoDelay = gClientSettings.showDetailedInfoDelay;
                suppressOnFocusChange = gClientSettings.suppressOnFocusChange;
                autoReconnectOnConnectionLost = gClientSettings.autoReconnectOnConnectionLost;
                forbidDuplicateForms = gClientSettings.forbidDuplicateForms;
                pivotOnlySelectedColumn = gClientSettings.pivotOnlySelectedColumn;
                matchSearchSeparator = gClientSettings.matchSearchSeparator;
                changeColorTheme(gClientSettings.colorTheme);
                colorPreferences = gClientSettings.colorPreferences;
                StyleDefaults.init();
                String language = gClientSettings.language;
                Document.get().getDocumentElement().setAttribute("lang", language);
                hasCapitalHyphensProblem = language != null && language.equals("en");
                dateFormat = gClientSettings.dateFormat;
                timeFormat = gClientSettings.timeFormat;
                dateTimeFormat = gClientSettings.dateFormat + " " + gClientSettings.timeFormat;
                preDefinedDateRangesNames = gClientSettings.preDefinedDateRangesNames;
                useTextAsFilterSeparator = gClientSettings.useTextAsFilterSeparator;
                userFiltersManualApplyMode = gClientSettings.userFiltersManualApplyMode;

                verticalNavbar = gClientSettings.verticalNavbar;

                disableActionsIfReadonly = gClientSettings.disableActionsIfReadonly;
                enableShowingRecentlyLogMessages = gClientSettings.enableShowingRecentlyLogMessages;
                pushNotificationPublicKey = gClientSettings.pushNotificationPublicKey;

                maxStickyLeft = gClientSettings.maxStickyLeft;

                jasperReportsIgnorePageMargins = gClientSettings.jasperReportsIgnorePageMargins;

                cssBackwardCompatibilityLevel = gClientSettings.cssBackwardCompatibilityLevel;

                initializeFrame(result.navigatorInfo, popupOwner);
                DateRangePickerBasedCellEditor.setPickerTwoDigitYearStart(gClientSettings.twoDigitYearStart);
            }
        });
    }

    private static native double getScale()/*-{
        return window.devicePixelRatio;
    }-*/;

    public static void openNavigatorMenu() {
        if (mobile) {
            mobileNavigatorView.openNavigatorMenu();
        }
    }
    
    public static void closeNavigatorMenu() {
        if (mobile) {
            mobileNavigatorView.closeNavigatorMenu();
        }
    }

    public void clean() {
        cleanRemote(() -> {}, false);
        GConnectionLostManager.invalidate();
        System.gc();
    }
}
