package platform.gwt.form2.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.base.client.ErrorAsyncCallback;
import platform.gwt.form2.client.dispatch.NavigatorDispatchAsync;
import platform.gwt.form2.client.form.FormPanel;
import platform.gwt.form2.client.navigator.GNavigatorController;
import platform.gwt.form2.client.window.WindowContainer;
import platform.gwt.form2.shared.actions.navigator.GetCommonWindows;
import platform.gwt.form2.shared.actions.navigator.GetCommonWindowsResult;
import platform.gwt.form2.shared.actions.navigator.GetNavigatorElements;
import platform.gwt.form2.shared.actions.navigator.GetNavigatorElementsResult;
import platform.gwt.form2.shared.view.window.GAbstractWindow;
import platform.gwt.form2.shared.view.window.GNavigatorWindow;

import java.util.*;

public class MainFrame implements EntryPoint {
    private final NavigatorDispatchAsync dispatcher = new NavigatorDispatchAsync();
    private WindowContainer mainContainer;

    private GNavigatorController navigatorController;
    private FormPanel formPanel;
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

        mainContainer = new WindowContainer() {
            @Override
            public Widget getWindowView(GAbstractWindow window) {
                Widget view;
                if (window.equals(formsWindow)) {
                    view = formPanel;
                } else if (window instanceof GNavigatorWindow) {
                    view = navigatorController.getNavigatorView((GNavigatorWindow) window).getView();
                } else {
                    view = commonWindows.get(window);
                }
                return view;
            }
        };

        navigatorController = new GNavigatorController() {
            @Override
            public void updateVisibility(Map<GAbstractWindow, Boolean> windows) {
                mainContainer.updateVisibility(windows);
            }

            @Override
            public void setInitialSize(GAbstractWindow window, int width, int height) {
                mainContainer.setInitialSize(window, width, height);
            }
        };
        formPanel = new FormPanel();

        dispatcher.execute(new GetCommonWindows(), new ErrorAsyncCallback<GetCommonWindowsResult>() {
            @Override
            public void success(GetCommonWindowsResult result) {
                formsWindow = result.forms;
                commonWindows.put(result.relevantForms, new Label(result.relevantForms.caption));
                commonWindows.put(result.relevantClasses, new Label(result.relevantClasses.caption));
                commonWindows.put(result.log, new Label(result.log.caption));
                commonWindows.put(result.status, new Label(result.status.caption));
            }
        });

        dispatcher.execute(new GetNavigatorElements(), new ErrorAsyncCallback<GetNavigatorElementsResult>() {
            @Override
            public void success(GetNavigatorElementsResult result) {

                navigatorController.initializeNavigatorViews(result.navigatorWindows);
                navigatorController.setRootElement(result.root);

                List<GAbstractWindow> allWindows = new ArrayList<GAbstractWindow>();
                allWindows.addAll(Arrays.asList(result.navigatorWindows));
                allWindows.addAll(commonWindows.keySet());
                mainContainer.initializeWindows(allWindows, formsWindow);
                RootLayoutPanel.get().add(mainContainer);

                navigatorController.update();
            }
        });
    }
}
