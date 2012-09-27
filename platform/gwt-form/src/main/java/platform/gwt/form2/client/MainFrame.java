package platform.gwt.form2.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import platform.gwt.form2.client.form.FormPanel;
import platform.gwt.form2.client.navigator.NavigatorPanel;

public class MainFrame implements EntryPoint {
    private FormPanel formPanel;
    private NavigatorPanel navigatorPanel;

    public void onModuleLoad() {
        GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
            @Override
            public void onUncaughtException(Throwable t) {
                GWT.log("Необработанная ошибка в GWT: ", t);
            }
        });

        // inject global styles
        GWT.<MainFrameResources>create(MainFrameResources.class).css().ensureInjected();

        navigatorPanel = new NavigatorPanel();
        formPanel = new FormPanel();

        SplitLayoutPanel main = new SplitLayoutPanel();
        main.addWest(navigatorPanel, 300);
        main.add(formPanel);

        RootLayoutPanel.get().add(main);
    }
}
