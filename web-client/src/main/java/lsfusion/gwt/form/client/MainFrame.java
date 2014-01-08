package lsfusion.gwt.form.client;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ErrorHandlingCallback;
import lsfusion.gwt.base.client.GwtClientUtils;
import lsfusion.gwt.base.shared.actions.BooleanResult;
import lsfusion.gwt.form.client.dispatch.NavigatorDispatchAsync;
import lsfusion.gwt.form.client.form.DefaultFormsController;
import lsfusion.gwt.form.client.form.dispatch.GwtActionDispatcher;
import lsfusion.gwt.form.client.form.ui.dialog.WindowHiddenHandler;
import lsfusion.gwt.form.client.log.GLog;
import lsfusion.gwt.form.client.navigator.GNavigatorAction;
import lsfusion.gwt.form.client.navigator.GNavigatorController;
import lsfusion.gwt.form.client.window.WindowsController;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.form.shared.actions.navigator.*;
import lsfusion.gwt.form.shared.view.GDefaultFormsType;
import lsfusion.gwt.form.shared.view.actions.GFormAction;
import lsfusion.gwt.form.shared.view.window.GAbstractWindow;
import lsfusion.gwt.form.shared.view.window.GModalityType;
import lsfusion.gwt.form.shared.view.window.GNavigatorWindow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainFrame implements EntryPoint {
    private final NavigatorDispatchAsync dispatcher = NavigatorDispatchAsync.Instance.get();
    public static boolean configurationAccessAllowed;

    private GNavigatorController navigatorController;
    private WindowsController windowsController;
    private DefaultFormsController formsController;

    public GAbstractWindow formsWindow;
    public Map<GAbstractWindow, Widget> commonWindows = new LinkedHashMap<GAbstractWindow, Widget>();

    public GNavigatorActionDispatcher actionDispatcher = new GNavigatorActionDispatcher();

    public void onModuleLoad() {
        GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
            @Override
            public void onUncaughtException(Throwable t) {
                GWT.log("Необработанная ошибка в GWT: ", t);
                Log.debug("Необработанная ошибка в GWT: ", t);
            }
        });

        // inject global styles
        GWT.<MainFrameResources>create(MainFrameResources.class).css().ensureInjected();

        hackForGwtDnd();

        formsController = new DefaultFormsController() {
            @Override
            public void executeNavigatorAction(GNavigatorAction action) {
                dispatcher.execute(new ExecuteNavigatorAction(action.sid), new ErrorHandlingCallback<ServerResponseResult>() {
                    @Override
                    public void success(ServerResponseResult result) {
                        actionDispatcher.dispatchResponse(result);
                    }
                });
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

        dispatcher.execute(new IsConfigurationAccessAllowedAction(), new ErrorHandlingCallback<BooleanResult>() {
            @Override
            public void success(BooleanResult result) {
                configurationAccessAllowed = result.value;
            }
        });

        dispatcher.execute(new ShowDefaultFormsAction(), new ErrorHandlingCallback<ShowDefaultFormsResult>() {
            @Override
            public void success(final ShowDefaultFormsResult result) {
                initializeWindows(result.defaultFormsType, result.defaultForms);
            }
        });
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

    private void initializeWindows(final GDefaultFormsType defaultFormsType, final ArrayList<String> defaultForms) {
        dispatcher.execute(new GetNavigatorInfo(), new ErrorHandlingCallback<GetNavigatorInfoResult>() {
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

                List<GAbstractWindow> allWindows = new ArrayList<GAbstractWindow>();
                allWindows.addAll(result.navigatorWindows);
                allWindows.addAll(commonWindows.keySet());

                boolean fullScreenMode = defaultFormsType == GDefaultFormsType.DEFAULT && !defaultForms.isEmpty();
                RootLayoutPanel.get().add(windowsController.initializeWindows(allWindows, formsWindow, fullScreenMode));

                navigatorController.update();

                openInitialForms(defaultForms);
            }
        });
    }

    private void openInitialForms(ArrayList<String> formsSIDs) {
        for (final String formSID : formsSIDs) {
            formsController.openForm(formSID, GModalityType.DOCKED, true);
        }
    }

    private class GNavigatorActionDispatcher extends GwtActionDispatcher {
        @Override
        protected void throwInServerInvocation(Throwable t, AsyncCallback<ServerResponseResult> callback) {
            dispatcher.execute(new ThrowInNavigatorAction(t), callback);
        }

        @Override
        protected void continueServerInvocation(Object[] actionResults, AsyncCallback<ServerResponseResult> callback) {
            dispatcher.execute(new ContinueNavigatorAction(actionResults), callback);
        }

        @Override
        public void execute(final GFormAction action) {
            if (action.modalityType.isModal()) {
                pauseDispatching();
            }
            formsController.openForm(action.form, action.modalityType, null, new WindowHiddenHandler() {
                @Override
                public void onHidden() {
                    if (action.modalityType.isModal()) {
                        continueDispatching();
                    }
                }
            });
        }
    }
}
