package lsfusion.gwt.form.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.GwtClientUtils;
import lsfusion.gwt.base.client.WrapperAsyncCallbackEx;
import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.gwt.base.shared.actions.BooleanResult;
import lsfusion.gwt.base.shared.actions.ListResult;
import lsfusion.gwt.base.shared.actions.VoidResult;
import lsfusion.gwt.form.client.dispatch.LSFusionDispatchAsync;
import lsfusion.gwt.form.client.dispatch.LogicsDispatchAsync;
import lsfusion.gwt.form.client.dispatch.NavigatorDispatchAsync;
import lsfusion.gwt.form.client.form.DefaultFormsController;
import lsfusion.gwt.form.client.form.ServerMessageProvider;
import lsfusion.gwt.form.client.form.dispatch.GwtActionDispatcher;
import lsfusion.gwt.form.client.form.ui.GFormController;
import lsfusion.gwt.form.client.form.ui.dialog.WindowHiddenHandler;
import lsfusion.gwt.form.client.log.GLog;
import lsfusion.gwt.form.client.navigator.GNavigatorAction;
import lsfusion.gwt.form.client.navigator.GNavigatorController;
import lsfusion.gwt.form.client.window.WindowsController;
import lsfusion.gwt.form.shared.actions.LookupLogics;
import lsfusion.gwt.form.shared.actions.logics.CreateNavigator;
import lsfusion.gwt.form.shared.actions.navigator.GetClientSettingsResult;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.form.shared.actions.navigator.*;
import lsfusion.gwt.form.shared.view.actions.GAction;
import lsfusion.gwt.form.shared.view.actions.GActivateFormAction;
import lsfusion.gwt.form.shared.view.actions.GFormAction;
import lsfusion.gwt.form.shared.view.actions.GMaximizeFormAction;
import lsfusion.gwt.form.shared.view.window.GAbstractWindow;
import lsfusion.gwt.form.shared.view.window.GNavigatorWindow;
import net.customware.gwt.dispatch.shared.Result;
import net.customware.gwt.dispatch.shared.general.StringResult;

import java.io.IOException;
import java.util.*;

// scope - every single tab (not browser) even for static
public class MainFrame implements EntryPoint, ServerMessageProvider {
    private static final MainFrameMessages messages = MainFrameMessages.Instance.get();

    public static LogicsDispatchAsync logicsDispatcher;
    public static NavigatorDispatchAsync navigatorDispatcher;

    public static boolean configurationAccessAllowed;
    public static boolean forbidDuplicateForms;
    public static boolean busyDialog;
    public static long busyDialogTimeout;
    public static String localeCookieName = "GWT_LOCALE";

    private GNavigatorController navigatorController;
    private WindowsController windowsController;
    private DefaultFormsController formsController;

    public GAbstractWindow formsWindow;
    public Map<GAbstractWindow, Widget> commonWindows = new LinkedHashMap<>();

    public GNavigatorActionDispatcher actionDispatcher = new GNavigatorActionDispatcher();

    private static Boolean shouldRepeatPingRequest = true;
    
    private final String tabSID = GwtSharedUtils.randomString(25);

    private LoadingManager loadingManager;

    @Override
    public void getServerActionMessage(ErrorHandlingCallback<StringResult> callback) {
        navigatorDispatcher.execute(new GetRemoteNavigatorActionMessage(), callback);
    }

    @Override
    public void getServerActionMessageList(ErrorHandlingCallback<ListResult> callback) {
        navigatorDispatcher.execute(new GetRemoteNavigatorActionMessageList(), callback);
    }

    @Override
    public void interrupt(boolean cancelable) {
        navigatorDispatcher.execute(new InterruptNavigator(cancelable), new ErrorHandlingCallback<VoidResult>());
    }

    public <T extends Result> void syncDispatch(final ExecuteNavigatorAction action, AsyncCallback<ServerResponseResult> callback) {
        //todo: возможно понадобится сделать чтото более сложное как в
        //todo: http://stackoverflow.com/questions/2061699/disable-user-interaction-in-a-gwt-container
        loadingManager.start();
        navigatorDispatcher.execute(action, new WrapperAsyncCallbackEx<ServerResponseResult>(callback) {
            @Override
            public void preProcess() {
                loadingManager.stop();
            }
        });
    }

    public void onModuleLoad() {
        initializeLogics();
    }

    public void initializeLogics() {
        String host = Window.Location.getParameter("host");
        String portString = Window.Location.getParameter("port");
        Integer port = portString != null ? Integer.valueOf(portString) : null;
        LSFusionDispatchAsync.instance.execute(new LookupLogics(host, port), new ErrorHandlingCallback<StringResult>() {
            @Override
            public void success(StringResult result) {
                logicsDispatcher = new LogicsDispatchAsync(result.get());
                initializeNavigator();
            }
        });
    }

    public void initializeNavigator() {
        logicsDispatcher.execute(new CreateNavigator(), new ErrorHandlingCallback<StringResult>() {
            @Override
            public void success(StringResult result) {
                navigatorDispatcher = new NavigatorDispatchAsync(result.get());
                initializeFrame();
            }
        });
    }

    public void initializeFrame() {
        Window.addWindowClosingHandler(new Window.ClosingHandler() { // добавляем после инициализации окон
            @Override
            public void onWindowClosing(Window.ClosingEvent event) {
                try {
                    if (windowsController != null) {
                        windowsController.storeWindowsSizes();
                    }
                } finally {
                    clean();
                }
            }
        });

        navigatorDispatcher.execute(new GetLocaleAction(), new ErrorHandlingCallback<StringResult>() {
            @Override
            public void success(StringResult result) {
                setLocale(result.get());
            }
        });

        GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
            @Override
            public void onUncaughtException(Throwable t) {
                GExceptionManager.logClientError("Uncaught GWT : " + messages.uncaughtGWTException() + ": ", t);
            }
        });

        // inject global styles
        GWT.<MainFrameResources>create(MainFrameResources.class).css().ensureInjected();

        hackForGwtDnd();

        formsController = new DefaultFormsController(tabSID) {
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
                        actionDispatcher.dispatchResponse(result);
                    }
                });
            }

            @Override
            public void executeNotificationAction(String actionSID, int type) {
                syncDispatch(new ExecuteNavigatorAction(actionSID, type), new ErrorHandlingCallback<ServerResponseResult>() {
                    @Override
                    public void success(ServerResponseResult result) {
                        actionDispatcher.dispatchResponse(result);
                    }
                });
            }

            @Override
            public void setCurrentForm(String formID) {
                navigatorDispatcher.execute(new SetCurrentForm(formID), new ErrorHandlingCallback<VoidResult>());
            }
        };

        windowsController = new WindowsController() {
            @Override
            public Widget getWindowView(GAbstractWindow window) {
                Widget view;
                if (window.equals(formsWindow)) {
                    view = formsController.getView();
                } else if (window instanceof GNavigatorWindow) {
                    view = navigatorController.getNavigatorView((GNavigatorWindow) window).getView();
                } else {
                    view = commonWindows.get(window);
                }
                return view;
            }
        };

        navigatorController = new GNavigatorController(formsController) {
            @Override
            public void updateVisibility(Map<GAbstractWindow, Boolean> windows) {
                windowsController.updateVisibility(windows);
            }

            @Override
            public void setInitialSize(GAbstractWindow window, int width, int height) {
                windowsController.setInitialSize(window, width, height);
            }
        };

        navigatorDispatcher.execute(new IsConfigurationAccessAllowedAction(), new ErrorHandlingCallback<BooleanResult>() {
            @Override
            public void success(BooleanResult result) {
                configurationAccessAllowed = result.value;
            }
        });

        navigatorDispatcher.execute(new ForbidDuplicateFormsAction(), new ErrorHandlingCallback<BooleanResult>() {
            @Override
            public void success(BooleanResult result) {
                forbidDuplicateForms = result.value;
            }
        });

        navigatorDispatcher.execute(new GetClientSettings(), new ErrorHandlingCallback<GetClientSettingsResult>() {
            @Override
            public void success(GetClientSettingsResult result) {
                busyDialog = result.busyDialog;
                busyDialogTimeout = Math.max(result.busyDialogTimeout - 500, 500); //минимальный таймаут 500мс + всё равно возникает задержка около 500мс
                loadingManager = busyDialog ? new GBusyDialogDisplayer(MainFrame.this) : new LoadingBlocker(MainFrame.this); // почему-то в busyDialog не работает showBusyDialog и blockingPanel
            }
        });

        initializeWindows();

        GConnectionLostManager.start();

        Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                if (shouldRepeatPingRequest && !GConnectionLostManager.shouldBeBlocked()) {
                    setShouldRepeatPingRequest(false);
                    navigatorDispatcher.execute(new ClientPushMessage(), new ErrorHandlingCallback<ClientMessageResult>() {
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

    private void setLocale(String newLocale) {
        String oldLocale = Cookies.getCookie(localeCookieName);
        if(oldLocale == null || !oldLocale.equals(newLocale)) {
            Cookies.setCookie(localeCookieName, newLocale, new Date(System.currentTimeMillis() + 1000L*60L*60L*24L*365L*5L));
            Window.Location.reload();
        }
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

    private void initializeWindows() {
        navigatorDispatcher.execute(new GetNavigatorInfo(), new ErrorHandlingCallback<GetNavigatorInfoResult>() {
            @Override
            public void success(GetNavigatorInfoResult result) {
                GwtClientUtils.removeLoaderFromHostedPage();

                formsWindow = result.forms;
                commonWindows.put(result.log, GLog.createLogPanel(result.log.visible));
                commonWindows.put(result.status, new Label(result.status.caption));

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
        navigatorDispatcher.execute(new CloseNavigator(), new ErrorHandlingCallback<VoidResult>());
        GConnectionLostManager.invalidate();
        System.gc();
    }

    private class GNavigatorActionDispatcher extends GwtActionDispatcher {
        @Override
        protected void throwInServerInvocation(Throwable t, AsyncCallback<ServerResponseResult> callback) {
            navigatorDispatcher.execute(new ThrowInNavigatorAction(t), callback);
        }

        @Override
        protected void continueServerInvocation(Object[] actionResults, AsyncCallback<ServerResponseResult> callback) {
            navigatorDispatcher.execute(new ContinueNavigatorAction(actionResults), callback);
        }

        @Override
        public void execute(final GFormAction action) {
            if (action.modalityType.isModal()) {
                pauseDispatching();
            }
            formsController.openForm(action.form, action.modalityType, action.forbidDuplicate, null, new WindowHiddenHandler() {
                @Override
                public void onHidden() {
                    if (action.modalityType.isModal()) {
                        continueDispatching();
                    }
                }
            });
        }

        @Override
        public void execute(final GActivateFormAction action) {
            formsController.selectTab(action.formCanonicalName);
        }

        @Override
        public void execute(final GMaximizeFormAction action) {
            windowsController.setFullScreenMode(true);
        }
    }
}
