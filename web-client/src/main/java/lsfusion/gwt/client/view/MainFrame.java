package lsfusion.gwt.client.view;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.action.GAction;
import lsfusion.gwt.client.action.GFormAction;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.WrapperAsyncCallbackEx;
import lsfusion.gwt.shared.GwtSharedUtils;
import lsfusion.gwt.shared.exceptions.AppServerNotAvailableException;
import lsfusion.gwt.shared.result.BooleanResult;
import lsfusion.gwt.shared.result.ListResult;
import lsfusion.gwt.shared.result.VoidResult;
import lsfusion.gwt.client.base.busy.GBusyDialogDisplayer;
import lsfusion.gwt.client.base.busy.LoadingBlocker;
import lsfusion.gwt.client.base.busy.LoadingManager;
import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.gwt.client.base.exception.AuthenticationDispatchException;
import lsfusion.gwt.client.base.exception.ErrorHandlingCallback;
import lsfusion.gwt.client.base.exception.GExceptionManager;
import lsfusion.gwt.client.base.log.GLog;
import lsfusion.gwt.client.base.result.ListResult;
import lsfusion.gwt.client.base.result.VoidResult;
import lsfusion.gwt.client.controller.dispatch.LogicsDispatchAsync;
import lsfusion.gwt.client.controller.remote.GConnectionLostManager;
import lsfusion.gwt.client.controller.remote.action.CreateNavigatorAction;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.navigator.*;
import lsfusion.gwt.client.form.controller.DefaultFormsController;
import lsfusion.gwt.client.form.controller.GFormController;
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

// scope - every single tab (not browser) even for static
public class MainFrame implements EntryPoint, ServerMessageProvider {
    private static final ClientMessages messages = ClientMessages.Instance.get();

    public static LogicsDispatchAsync logicsDispatchAsync;
    public static NavigatorDispatchAsync navigatorDispatchAsync;

    // settings    
    public static boolean devMode;
    public static boolean configurationAccessAllowed;
    public static boolean forbidDuplicateForms;
    public static boolean busyDialog;
    public static long busyDialogTimeout;
    private static Boolean shouldRepeatPingRequest = true;

    private final String tabSID = GwtSharedUtils.randomString(25);

    private LoadingManager loadingManager;

    @Override
    public void getServerActionMessage(ErrorHandlingCallback<StringResult> callback) {
        navigatorDispatchAsync.execute(new GetRemoteNavigatorActionMessage(), callback);
    }

    @Override
    public void getServerActionMessageList(ErrorHandlingCallback<ListResult> callback) {
        navigatorDispatchAsync.execute(new GetRemoteNavigatorActionMessageList(), callback);
    }

    @Override
    public void interrupt(boolean cancelable) {
        navigatorDispatchAsync.execute(new InterruptNavigator(cancelable), new ErrorHandlingCallback<VoidResult>());
    }

    public <T extends Result> void syncDispatch(final ExecuteNavigatorAction action, AsyncCallback<ServerResponseResult> callback) {
        //todo: возможно понадобится сделать чтото более сложное как в
        //todo: http://stackoverflow.com/questions/2061699/disable-user-interaction-in-a-gwt-container
        loadingManager.start();
        navigatorDispatchAsync.execute(action, new WrapperAsyncCallbackEx<ServerResponseResult>(callback) {
            @Override
            public void preProcess() {
                loadingManager.stop();
            }
        });
    }

    public void onModuleLoad() {
        // inject global styles
        GWT.<MainFrameResources>create(MainFrameResources.class).css().ensureInjected();

        hackForGwtDnd();
        
        initializeLogicsAndNavigator(0);
    }

    public void initializeLogicsAndNavigator(final int attemptCount) {
        String host = Window.Location.getParameter("host");
        String portString = Window.Location.getParameter("port");
        Integer port = portString != null ? Integer.valueOf(portString) : null;
        String exportName = Window.Location.getParameter("exportName");
        logicsDispatchAsync = new LogicsDispatchAsync(host, port, exportName);
        logicsDispatchAsync.execute(new CreateNavigatorAction(), new ErrorHandlingCallback<StringResult>() {
            @Override
            public void success(StringResult result) {
                navigatorDispatchAsync = new NavigatorDispatchAsync(result.get());
                initializeFrame();
            }

            @Override
            public void failure(Throwable caught) {
                if(caught instanceof AuthenticationDispatchException) { // token is invalid, then we need to relogin (and actually need to logout, to reauthenticate and get new token) - it's the only place on client where token is checked
                    GwtClientUtils.logout();
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
    
    private static class Linker<T> {
        private T link;
    }

    public void initializeFrame() {
        GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
            @Override
            public void onUncaughtException(Throwable t) {
                GExceptionManager.logClientError(t);
            }
        });

        final Linker<GNavigatorActionDispatcher> actionDispatcherLink = new Linker<>();
        final DefaultFormsController formsController = new DefaultFormsController(tabSID) {
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

            @Override
            public void setCurrentForm(String formID) {
                navigatorDispatchAsync.execute(new SetCurrentForm(formID), new ErrorHandlingCallback<VoidResult>());
            }
        };

        final Linker<GAbstractWindow> formsWindowLink = new Linker<>();
        final Linker<Map<GAbstractWindow, Widget>> commonWindowsLink = new Linker<>();        
        final Linker<GNavigatorController> navigatorControllerLink = new Linker<>();
        final WindowsController windowsController = new WindowsController() {
            @Override
            public Widget getWindowView(GAbstractWindow window) {
                Widget view;
                if (window.equals(formsWindowLink.link)) {
                    view = formsController.getView();
                } else if (window instanceof GNavigatorWindow) {
                    view = navigatorControllerLink.link.getNavigatorView((GNavigatorWindow) window).getView();
                } else {
                    view = commonWindowsLink.link.get(window);
                }
                return view;
            }
        };

        actionDispatcherLink.link = new GNavigatorActionDispatcher(windowsController, formsController);

        Window.addWindowClosingHandler(new Window.ClosingHandler() { // добавляем после инициализации окон
            @Override
            public void onWindowClosing(Window.ClosingEvent event) {
                try {
                    windowsController.storeWindowsSizes();
                } finally {
                    clean();
                }
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

        navigatorDispatchAsync.execute(new GetClientSettings(), new ErrorHandlingCallback<GetClientSettingsResult>() {
            @Override
            public void success(GetClientSettingsResult result) {
                busyDialog = result.busyDialog;
                busyDialogTimeout = Math.max(result.busyDialogTimeout - 500, 500); //минимальный таймаут 500мс + всё равно возникает задержка около 500мс
                loadingManager = busyDialog ? new GBusyDialogDisplayer(MainFrame.this) : new LoadingBlocker(MainFrame.this); // почему-то в busyDialog не работает showBusyDialog и blockingPanel
                devMode = result.devMode;
                configurationAccessAllowed = result.configurationAccessAllowed;
                forbidDuplicateForms = result.forbidDuplicateForms;
            }
        });

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
                                GFormController form = result.currentForm == null ? null : formsController.getForm(result.currentForm);
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

    private void initializeWindows(final DefaultFormsController formsController, final WindowsController windowsController, final GNavigatorController navigatorController, final Linker<GAbstractWindow> formsWindowLink, final Linker<Map<GAbstractWindow, Widget>> commonWindowsLink) {
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

                RootLayoutPanel.get().add(windowsController.initializeWindows(allWindows, formsWindow));

                navigatorController.update();

                formsController.executeNotificationAction("SystemEvents.onWebClientStarted[]", 0);
            }
        });
    }

    public void clean() {
        navigatorDispatchAsync.execute(new CloseNavigator(), new ErrorHandlingCallback<VoidResult>());
        GConnectionLostManager.invalidate();
        System.gc();
    }

}
