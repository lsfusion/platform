package platform.gwt.form.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.form.client.dispatch.NavigatorDispatchAsync;
import platform.gwt.form.client.form.DefaultFormsController;
import platform.gwt.form.client.log.GLog;
import platform.gwt.form.client.navigator.GNavigatorController;
import platform.gwt.form.client.window.WindowsController;
import platform.gwt.form.shared.actions.navigator.GetNavigatorInfo;
import platform.gwt.form.shared.actions.navigator.GetNavigatorInfoResult;
import platform.gwt.form.shared.view.window.GAbstractWindow;
import platform.gwt.form.shared.view.window.GNavigatorWindow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainFrame implements EntryPoint {
    private final NavigatorDispatchAsync dispatcher = new NavigatorDispatchAsync();

    private GNavigatorController navigatorController;
    private WindowsController windowsController;
    private DefaultFormsController formsController;

    public GAbstractWindow formsWindow;
    public Map<GAbstractWindow, Widget> commonWindows = new LinkedHashMap<GAbstractWindow, Widget>();

    public void onModuleLoad() {
        GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
            @Override
            public void onUncaughtException(Throwable t) {
                GWT.log("Необработанная ошибка в GWT: ", t);
            }
        });

        // inject global styles
        GWT.<MainFrameResources>create(MainFrameResources.class).css().ensureInjected();

        formsController = new DefaultFormsController();

        windowsController = new WindowsController() {
            @Override
            public Widget getWindowView(GAbstractWindow window) {
                Widget view;
                if (window.equals(formsWindow)) {
                    view = formsController;
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

        HotkeyManager.get().install(windowsController);

        initCommonWindows();
    }

    private void initCommonWindows() {
        dispatcher.execute(new GetNavigatorInfo(), new ErrorHandlingCallback<GetNavigatorInfoResult>() {
            @Override
            public void success(GetNavigatorInfoResult result) {
                formsWindow = result.forms;
                commonWindows.put(result.relevantForms, new Label(result.relevantForms.caption));
                commonWindows.put(result.relevantClasses, new Label(result.relevantClasses.caption));
                commonWindows.put(result.log, GLog.createLogPanel(result.log.visible));
                commonWindows.put(result.status, new Label(result.status.caption));

                // пока прячем всё, что не поддерживается
                result.status.visible = false;
                result.relevantClasses.visible = false;
                result.relevantForms.visible = false;

                navigatorController.initializeNavigatorViews(result.navigatorWindows);
                navigatorController.setRootElement(result.root);

                List<GAbstractWindow> allWindows = new ArrayList<GAbstractWindow>();
                allWindows.addAll(result.navigatorWindows);
                allWindows.addAll(commonWindows.keySet());
                windowsController.initializeWindows(allWindows, formsWindow);
                RootLayoutPanel.get().add(windowsController);

                navigatorController.update();
            }
        });
    }
}
