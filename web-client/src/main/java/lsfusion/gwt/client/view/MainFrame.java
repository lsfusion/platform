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
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.action.GAction;
import lsfusion.gwt.client.action.GFormAction;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.WrapperAsyncCallbackEx;
import lsfusion.gwt.client.base.busy.GBusyDialogDisplayer;
import lsfusion.gwt.client.base.busy.LoadingBlocker;
import lsfusion.gwt.client.base.busy.LoadingManager;
import lsfusion.gwt.client.base.exception.*;
import lsfusion.gwt.client.base.log.GLog;
import lsfusion.gwt.client.base.result.ListResult;
import lsfusion.gwt.client.base.result.VoidResult;
import lsfusion.gwt.client.base.view.DialogBoxHelper;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.controller.dispatch.LogicsDispatchAsync;
import lsfusion.gwt.client.controller.remote.GConnectionLostManager;
import lsfusion.gwt.client.controller.remote.action.CreateNavigatorAction;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.navigator.*;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.event.GMouseStroke;
import lsfusion.gwt.client.form.object.table.grid.user.design.GColorPreferences;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.navigator.ConnectionInfo;
import lsfusion.gwt.client.navigator.GNavigatorAction;
import lsfusion.gwt.client.navigator.controller.GNavigatorController;
import lsfusion.gwt.client.navigator.controller.dispatch.GNavigatorActionDispatcher;
import lsfusion.gwt.client.navigator.controller.dispatch.NavigatorDispatchAsync;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;
import lsfusion.gwt.client.navigator.window.view.WindowsController;
import net.customware.gwt.dispatch.shared.Result;
import net.customware.gwt.dispatch.shared.general.StringResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static lsfusion.gwt.client.base.view.grid.AbstractDataGridBuilder.IGNORE_DBLCLICK_CHECK;

// scope - every single tab (not browser) even for static
public class MainFrame implements EntryPoint, ServerMessageProvider {
    private static final ClientMessages messages = ClientMessages.Instance.get();

    public static LogicsDispatchAsync logicsDispatchAsync;
    public static NavigatorDispatchAsync navigatorDispatchAsync;

    public static boolean mobile;

    // settings    
    public static boolean devMode;
    public static String projectLSFDir;
    public static boolean showDetailedInfo;
    public static boolean forbidDuplicateForms;
    public static boolean busyDialog;
    public static long busyDialogTimeout;
    public static long updateRendererStateSetTimeout = 100;
    public static boolean showNotDefinedStrings;
    public static boolean pivotOnlySelectedColumn;
    private static Boolean shouldRepeatPingRequest = true;
    public static boolean disableConfirmDialog = false;
    
    public static GColorTheme colorTheme = GColorTheme.DEFAULT;
    public static List<ColorThemeChangeListener> colorThemeChangeListeners = new ArrayList<>(); 
    
    public static GColorPreferences colorPreferences;

    public static String dateFormat;
    public static String timeFormat;

    private LoadingManager loadingManager;

    @Override
    public void getServerActionMessage(ErrorHandlingCallback<StringResult> callback) {
        navigatorDispatchAsync.executePriority(new GetRemoteNavigatorActionMessage(), callback);
    }

    @Override
    public void getServerActionMessageList(ErrorHandlingCallback<ListResult> callback) {
        navigatorDispatchAsync.executePriority(new GetRemoteNavigatorActionMessageList(), callback);
    }

    @Override
    public void interrupt(boolean cancelable) {
        navigatorDispatchAsync.executePriority(new InterruptNavigator(cancelable), new ErrorHandlingCallback<VoidResult>());
    }

    public <T extends Result> void syncDispatch(final ExecuteNavigatorAction action, AsyncCallback<ServerResponseResult> callback) {
        //todo: may be need something more sophisticated
        //todo: http://stackoverflow.com/questions/2061699/disable-user-interaction-in-a-gwt-container
        loadingManager.start();
        navigatorDispatchAsync.execute(action, new WrapperAsyncCallbackEx<ServerResponseResult>(callback) {
            @Override
            public void preProcess() {
                loadingManager.stop();
            }
        });
    }

    public static void cleanRemote(Runnable runnable, boolean connectionLost) {
        if (navigatorDispatchAsync != null && !connectionLost) { // dispatcher may be not initialized yet (at first look up logics call)
            navigatorDispatchAsync.execute(new CloseNavigator(), new AsyncCallback<VoidResult>() {
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
        hackForGwtDnd();
        
        GWT.setUncaughtExceptionHandler(t -> {
            GExceptionManager.logClientError(t);
            DialogBoxHelper.showMessageBox(true, "Error", t.getMessage(), null);
        });

        initializeLogicsAndNavigator(0);
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
            lastBlurredElement.focus();
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
                switchedToAnotherWindow = isSwitchedToAnotherWindow(event);
                return !switchedToAnotherWindow;
            }
        }
        return true;
    }

    //heuristic
    //'visibilitychange' will not work, because 'focus' event is caught by editor earlier then by whole document
    //(https://stackoverflow.com/questions/28993157/visibilitychange-event-is-not-triggered-when-switching-program-window-with-altt)
    private static native boolean isSwitchedToAnotherWindow(Event event) /*-{
        return event.relatedTarget == null && event.sourceCapabilities == null;
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
            if(beforeLastClickedTarget != null && lastClickedTarget != null && target == lastClickedTarget && beforeLastClickedTarget != lastClickedTarget && noIgnoreDblClickCheck(lastClickedTarget))
                return false;
        }
        return true;
    }

    //lastClickedTarget and beforeLastClickedTarget can be not equal if we change element at first click
    private static boolean noIgnoreDblClickCheck(Element element) {
        return GwtClientUtils.getParentWithAttribute(element, IGNORE_DBLCLICK_CHECK) == null;
    }

    public void initializeFrame() {
        currentForm = null;

        // we need to read settings first to have loadingManager set (for syncDispatch)
        navigatorDispatchAsync.execute(new GetClientSettings(), new ErrorHandlingCallback<GetClientSettingsResult>() {
            @Override
            public void success(GetClientSettingsResult result) {
                busyDialog = result.busyDialog;
                busyDialogTimeout = Math.max(result.busyDialogTimeout - 500, 500); //минимальный таймаут 500мс + всё равно возникает задержка около 500мс
                loadingManager = busyDialog ? new GBusyDialogDisplayer(MainFrame.this) : new LoadingBlocker(MainFrame.this); // почему-то в busyDialog не работает showBusyDialog и blockingPanel
                devMode = result.devMode;
                projectLSFDir = result.projectLSFDir;
                showDetailedInfo = result.showDetailedInfo;
                forbidDuplicateForms = result.forbidDuplicateForms;
                showNotDefinedStrings = result.showNotDefinedStrings;
                pivotOnlySelectedColumn = result.pivotOnlySelectedColumn;
                changeColorTheme(result.colorTheme);
                colorPreferences = result.colorPreferences;
                StyleDefaults.appendClientSettingsCSS();
                dateFormat = result.dateFormat;
                timeFormat = result.timeFormat;
            }
        });

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
                    view = navigatorControllerLink.link.getNavigatorView((GNavigatorWindow) window).getView();
                } else {
                    view = commonWindowsLink.link.get(window);
                }
                return view;
            }
        };

        final Linker<GNavigatorActionDispatcher> actionDispatcherLink = new Linker<>();
        final FormsController formsController = new FormsController(windowsController) {
            @Override
            public void executeNavigatorAction(GNavigatorAction action, final NativeEvent event) {
                syncDispatch(new ExecuteNavigatorAction(action.canonicalName, 1), new ErrorHandlingCallback<ServerResponseResult>() {
                    @Override
                    public void success(ServerResponseResult result) {
                        if(event.getCtrlKey()) {
                            for (GAction action : result.actions) // хак, но весь механизм forbidDuplicate один большой хак
                                if (action instanceof GFormAction)
                                    ((GFormAction) action).forbidDuplicate = false;
                        }
                        actionDispatcherLink.link.dispatchResponse(result);
                        setLastCompletedRequest(result.requestIndex);
                    }
                });
            }

            @Override
            public void executeNotificationAction(String actionSID, int type) {
                syncDispatch(new ExecuteNavigatorAction(actionSID, type), new ErrorHandlingCallback<ServerResponseResult>() {
                    @Override
                    public void success(ServerResponseResult result) {
                        actionDispatcherLink.link.dispatchResponse(result);
                    }
                });
            }
        };

        formsControllerLinker.link = formsController;
        actionDispatcherLink.link = new GNavigatorActionDispatcher(windowsController, formsController);

        //we use CloseHandler instead of Window.ClosingHandler because mobile browsers send closing event without closing window
        Window.addCloseHandler(new CloseHandler<Window>() { // добавляем после инициализации окон
            @Override
            public void onClose(CloseEvent event) {
                try {
                    windowsController.storeWindowsSizes();
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

            @Override
            public void setInitialSize(GAbstractWindow window, int width, int height) {
                windowsController.setInitialSize(window, width, height);
            }
        };
        navigatorControllerLink.link = navigatorController;

        initializeWindows(formsController, windowsController, navigatorController, formsWindowLink, commonWindowsLink);

        GConnectionLostManager.start();

        Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                if (shouldRepeatPingRequest && !GConnectionLostManager.shouldBeBlocked()) {
                    setShouldRepeatPingRequest(false);
                    navigatorDispatchAsync.execute(new ClientPushMessage(), new ErrorHandlingCallback<ClientMessageResult>() {
                        @Override
                        public void success(ClientMessageResult result) {
                            setShouldRepeatPingRequest(true);
                            super.success(result);
                            for (Integer idNotification : result.notificationList) {
                                FormContainer<?> currentForm = MainFrame.getCurrentForm();
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
                        public void failure(Throwable caught) {
                            setShouldRepeatPingRequest(true);
                            super.failure(caught);
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
            cssLink.setAttribute("href", "static/css/" + colorTheme.getSid() + ".css");
            
            StyleDefaults.reset();

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

    private static FormContainer<?> currentForm;
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

    private void initializeWindows(final FormsController formsController, final WindowsController windowsController, final GNavigatorController navigatorController, final Linker<GAbstractWindow> formsWindowLink, final Linker<Map<GAbstractWindow, Widget>> commonWindowsLink) {
        navigatorDispatchAsync.execute(new GetNavigatorInfo(), new ErrorHandlingCallback<GetNavigatorInfoResult>() {
            @Override
            public void success(GetNavigatorInfoResult result) {
                GwtClientUtils.removeLoaderFromHostedPage();

                GAbstractWindow formsWindow = result.forms;
                formsWindowLink.link = formsWindow;
                Map<GAbstractWindow, Widget> commonWindows = new LinkedHashMap<>();
                commonWindows.put(result.log, GLog.createLogPanel(result.log.visible));
                commonWindows.put(result.status, new Label(result.status.caption));
                commonWindowsLink.link = commonWindows;

                // пока прячем всё, что не поддерживается
                result.status.visible = false;

                navigatorController.initializeNavigatorViews(result.navigatorWindows);
                navigatorController.setRootElement(result.root);

                List<GAbstractWindow> allWindows = new ArrayList<>();
                allWindows.addAll(result.navigatorWindows);
                allWindows.addAll(commonWindows.keySet());

                windowsController.initializeWindows(allWindows, formsWindow);
                formsController.initRoot(formsController);

                navigatorController.update();

                formsController.executeNotificationAction("SystemEvents.onClientStarted[]", 0);
            }
        });
    }

    public void initializeLogicsAndNavigator(final int attemptCount) {
        String host = Window.Location.getParameter("host");
        String portString = Window.Location.getParameter("port");
        Integer port = portString != null ? Integer.valueOf(portString) : null;
        String exportName = Window.Location.getParameter("exportName");
        Integer screenWidth = Window.getClientWidth();
        Integer screenHeight = Window.getClientHeight();
        mobile = screenWidth <= StyleDefaults.maxMobileWidth;
        logicsDispatchAsync = new LogicsDispatchAsync(host, port, exportName);
        logicsDispatchAsync.execute(new CreateNavigatorAction(new ConnectionInfo(screenWidth + "x" + screenHeight, mobile)), new ErrorHandlingCallback<StringResult>() {
            @Override
            public void success(StringResult result) {
                navigatorDispatchAsync = new NavigatorDispatchAsync(result.get());
                initializeFrame();
            }

            @Override
            public void failure(Throwable caught) {
                if(caught instanceof AuthenticationDispatchException) { // token is invalid, then we need to relogin (and actually need to logout, to reauthenticate and get new token) - it's the only place on client where token is checked
                    GwtClientUtils.logout();
                } else if(caught instanceof RemoteMessageDispatchException) {
                    GwtClientUtils.logout(); //see issue #312
                } else if(caught instanceof AppServerNotAvailableDispatchException) {
                    new Timer()
                    {
                        @Override
                        public void run()
                        {
                            GwtClientUtils.setAttemptCount(attemptCount + 1);
                            initializeLogicsAndNavigator(attemptCount + 1);
                        }
                    }.schedule(2000);
                } else {
                    super.failure(caught);
                }
            }
        });
    }

    public void clean() {
        cleanRemote(() -> {}, false);
        GConnectionLostManager.invalidate();
        System.gc();
    }
}
