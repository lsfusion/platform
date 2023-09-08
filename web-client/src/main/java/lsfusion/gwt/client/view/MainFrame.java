package lsfusion.gwt.client.view;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.*;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
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
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.controller.dispatch.LogicsDispatchAsync;
import lsfusion.gwt.client.controller.remote.GConnectionLostManager;
import lsfusion.gwt.client.controller.remote.action.PriorityAsyncCallback;
import lsfusion.gwt.client.controller.remote.action.PriorityErrorHandlingCallback;
import lsfusion.gwt.client.controller.remote.action.RequestAsyncCallback;
import lsfusion.gwt.client.controller.remote.action.RequestCountingAsyncCallback;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.navigator.*;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.event.GMouseStroke;
import lsfusion.gwt.client.form.object.table.grid.user.design.GColorPreferences;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.navigator.controller.GNavigatorController;
import lsfusion.gwt.client.navigator.controller.dispatch.GNavigatorActionDispatcher;
import lsfusion.gwt.client.navigator.controller.dispatch.NavigatorDispatchAsync;
import lsfusion.gwt.client.navigator.view.BSMobileNavigatorView;
import lsfusion.gwt.client.navigator.view.ExcelMobileNavigatorView;
import lsfusion.gwt.client.navigator.view.MobileNavigatorView;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;
import lsfusion.gwt.client.navigator.window.GToolbarNavigatorWindow;
import lsfusion.gwt.client.navigator.window.view.WindowsController;
import net.customware.gwt.dispatch.shared.Result;

import java.io.IOException;
import java.util.*;

import static lsfusion.gwt.client.base.BaseImage.IMAGE_WIDGET;

// scope - every single tab (not browser) even for static
public class MainFrame implements EntryPoint {
    public static LogicsDispatchAsync logicsDispatchAsync;
    public static NavigatorDispatchAsync navigatorDispatchAsync;

    public static boolean mobile;
    public static MobileNavigatorView mobileNavigatorView = null;
    public static int mobileAdjustment;

    public static boolean firefox;

    // settings    
    public static boolean devMode;
    public static String projectLSFDir;
    public static boolean showDetailedInfo;
    public static boolean autoReconnectOnConnectionLost;
    public static int showDetailedInfoDelay;
    public static boolean forbidDuplicateForms;
    public static boolean useBootstrap;
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

    public static String dateFormat;
    public static String timeFormat;
    public static String dateTimeFormat;
    public static String[] preDefinedDateRangesNames;

    public static boolean useTextAsFilterSeparator;

    public static boolean verticalNavbar;

    public static boolean userFiltersManualApplyMode;

    // async dispatch
    public <T extends Result> long asyncDispatch(final ExecuteNavigatorAction action, RequestCountingAsyncCallback<ServerResponseResult> callback) {
        return navigatorDispatchAsync.asyncExecute(action, callback);
    }

    public <T extends Result> long syncDispatch(final ExecuteNavigatorAction action, RequestCountingAsyncCallback<ServerResponseResult> callback) {
        return syncDispatch(action, callback, false);
    }

    public static <T extends Result> long syncDispatch(final NavigatorRequestAction action, RequestAsyncCallback<ServerResponseResult> callback, boolean continueInvocation) {
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

        hackForGwtDnd();

        GwtClientUtils.setZeroZIndex(RootLayoutPanel.get().getElement());

        GWT.setUncaughtExceptionHandler(t -> {
            if (!ignoreException(t)) {
                GExceptionManager.logClientError(t);
                DialogBoxHelper.showMessageBox(true, "Error", t.getMessage(), null);
            }
        });

        initializeLogicsAndNavigator();
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

    private static Element lastBlurredElement;
    // Event.addNativePreviewHandler(this::previewNativeEvent); doesn't work since only mouse events are propagated see DOM.previewEvent(evt) usages (only mouse and keyboard events are previewed);
    // this solution is not pretty clean since not all events are previewed, but for now, works pretty good
    public static void setLastBlurredElement(Element lastBlurredElement) {
        MainFrame.lastBlurredElement = lastBlurredElement;
    }
    public static Element getLastBlurredElement() {
        return lastBlurredElement;
    }
    public static boolean focusLastBlurredElement(EventHandler focusEventHandler, Element focusEventElement) {
        // in theory we also have to check if focused element still visible, isShowing in GwtClientUtils but now it's assumed that it is always visible
        if(lastBlurredElement != null && lastBlurredElement != focusEventElement) { // return focus back where it was
            focusEventHandler.consume();
            FocusUtils.focus(lastBlurredElement, FocusUtils.Reason.RESTOREFOCUS);
            return true;
        }
        return false;
    }

    public static boolean previewEvent(Element target, Event event, boolean isEditing) {
        return previewFocusEvent(event, isEditing) && previewClickEvent(target, event);
    }

    private static boolean switchedToAnotherWindow;
    private static boolean previewFocusEvent(Event event, boolean isEditing) {
        if (isEditing) {
            if (BrowserEvents.FOCUS.equals(event.getType())) {
                if (switchedToAnotherWindow) {
                    switchedToAnotherWindow = false;
                    return false;
                }
            } else if (BrowserEvents.BLUR.equals(event.getType())) {
                switchedToAnotherWindow = isSwitchedToAnotherWindow(event, Document.get());
                return !switchedToAnotherWindow;
            }
        }
        return true;
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
    private static boolean previewClickEvent(Element target, Event event) {
        if (GMouseStroke.isClickEvent(event))
            if (event != lastClickedEvent) { // checking lastClickedEvent since it can be propagated (or not)
                lastClickedEvent = event;
                beforeLastClickedTarget = lastClickedTarget;
                lastClickedTarget = target;
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

    public void initializeFrame(NavigatorInfo result) {
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
            public <T extends Result> long syncDispatch(ExecuteNavigatorAction action, RequestCountingAsyncCallback<ServerResponseResult> callback) {
                return MainFrame.this.syncDispatch(action, callback);
            }

            @Override
            public <T extends Result> long asyncDispatch(ExecuteNavigatorAction action, RequestCountingAsyncCallback<ServerResponseResult> callback) {
                return MainFrame.this.asyncDispatch(action, callback);
            }

            @Override
            public GNavigatorActionDispatcher getDispatcher() {
                return actionDispatcherLink.link;
            }
        };

        formsControllerLinker.link = formsController;

        //we use CloseHandler instead of Window.ClosingHandler because mobile browsers send closing event without closing window
        Window.addCloseHandler(new CloseHandler<Window>() { // добавляем после инициализации окон
            @Override
            public void onClose(CloseEvent event) {
                try {
                    if (!mobile) {
                        windowsController.storeWindowsSizes();
                    }
                    windowsController.storeEditMode();
                } finally {
                    clean();
                }
            }
        });

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
                    navigatorDispatchAsync.executePriority(new ClientPushMessage(), new PriorityErrorHandlingCallback<ClientMessageResult>() {
                        @Override
                        public void onSuccess(ClientMessageResult result) {
                            setShouldRepeatPingRequest(true);
                            for (Integer idNotification : result.notificationList) {
                                FormContainer currentForm = MainFrame.getCurrentForm();
                                GFormController form = currentForm != null ? currentForm.getForm() : null;
                                if (form != null)
                                    try {
                                        form.executeNotificationAction(idNotification);
                                    } catch (IOException e) {
                                        GWT.log(e.getMessage());
                                    }
                                else {
                                    formsController.executeNotificationAction(String.valueOf(idNotification), 2);
                                }
                            }
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

        GExceptionManager.flushUnreportedThrowables();
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
                Pair<BaseImage, Boolean> baseImage = (Pair<BaseImage, Boolean>) imgTextElement.getPropertyObject(IMAGE_WIDGET);
                if(baseImage != null && baseImage.first instanceof BaseStaticImage) // if it uses themed image
                    BaseImage.updateImage(baseImage.first, imgTextElement, baseImage.second);
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

        FormsController.setGlobalClassName(true, useBootstrap ? "nav-bootstrap" : "nav-excel");
        FormsController.setGlobalClassName(true, mobile ? "nav-mobile" : "nav-desktop");

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
                    if (window instanceof GToolbarNavigatorWindow) {
                        GToolbarNavigatorWindow toolbarWindow = (GToolbarNavigatorWindow) window;
                        if (toolbarWindow.isInRootNavBar()) {
                            toolbarWindow.vertical = true;
                            toolbarWindow.verticalTextPosition = GToolbarNavigatorWindow.CENTER;
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

        formsController.executeNotificationAction("SystemEvents.onClientStarted[]", 0, formsController.new ServerResponseCallback(false) {
            @Override
            protected Runnable getOnRequestFinished() {
                return () -> {
                    if (formsController.getFormsCount() == 0) {
                        openNavigatorMenu();
                    }
                };
            }
        });
    }

    public static void applyNavigatorChanges(GNavigatorChangesDTO navigatorChangesDTO, GNavigatorController navigatorController, WindowsController windowsController) {
        if (navigatorChangesDTO.properties.length == 0) { // optimization
            return;
        }

        for(int i = 0; i < navigatorChangesDTO.properties.length; i++) {
            navigatorChangesDTO.properties[i].update(navigatorController, windowsController, navigatorChangesDTO.values[i]);
        }

        // here we do not do incremental update, but global "refresh" (as we use the same mechanism for selected mechanism)
        if (!mobile) {
            navigatorController.update();
        }
    }

    private native String getSessionId() /*-{
        return $wnd.document.body.getAttribute("sessionID");
    }-*/;

    public void initializeLogicsAndNavigator() {
        String portString = Window.Location.getParameter("port");
        Integer screenWidth = Window.getClientWidth();
        Integer screenHeight = Window.getClientHeight();
        mobile = Math.min(screenHeight, screenWidth) <= StyleDefaults.maxMobileWidthHeight;
        mobileAdjustment = mobile ? 1 : 0;

        logicsDispatchAsync = new LogicsDispatchAsync(Window.Location.getParameter("host"), portString != null ? Integer.valueOf(portString) : null,
                Window.Location.getParameter("exportName"));

        navigatorDispatchAsync = new NavigatorDispatchAsync(getSessionId());
        navigatorDispatchAsync.executePriority(new InitializeNavigator(screenWidth + "x" + screenHeight, mobile), new PriorityErrorHandlingCallback<InitializeNavigatorResult>() {
            @Override
            public void onSuccess(InitializeNavigatorResult result) {
                GClientSettings gClientSettings = result.gClientSettings;

                versionedColorThemesCss = gClientSettings.versionedColorThemesCss;
                busyDialogTimeout = Math.max(gClientSettings.busyDialogTimeout - 500, 500); // minimum timeout 500ms + there is still a delay of about 500ms
                staticImagesURL = gClientSettings.staticImagesURL;
                useBootstrap = gClientSettings.useBootstrap;
                devMode = gClientSettings.devMode;
                projectLSFDir = gClientSettings.projectLSFDir;
                showDetailedInfo = gClientSettings.showDetailedInfo;
                showDetailedInfoDelay = gClientSettings.showDetailedInfoDelay;
                autoReconnectOnConnectionLost = gClientSettings.autoReconnectOnConnectionLost;
                forbidDuplicateForms = gClientSettings.forbidDuplicateForms;
                pivotOnlySelectedColumn = gClientSettings.pivotOnlySelectedColumn;
                matchSearchSeparator = gClientSettings.matchSearchSeparator;
                changeColorTheme(gClientSettings.colorTheme);
                colorPreferences = gClientSettings.colorPreferences;
                StyleDefaults.init();
                dateFormat = gClientSettings.dateFormat;
                timeFormat = gClientSettings.timeFormat;
                dateTimeFormat = gClientSettings.dateFormat + " " + gClientSettings.timeFormat;
                preDefinedDateRangesNames = gClientSettings.preDefinedDateRangesNames;
                useTextAsFilterSeparator = gClientSettings.useTextAsFilterSeparator;
                userFiltersManualApplyMode = gClientSettings.userFiltersManualApplyMode;

                verticalNavbar = gClientSettings.verticalNavbar;

                initializeFrame(result.navigatorInfo);
            }
        });
    }
    
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
