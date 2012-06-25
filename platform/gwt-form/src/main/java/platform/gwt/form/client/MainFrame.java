package platform.gwt.form.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import platform.gwt.base.client.GwtClientUtils;
import platform.gwt.form.client.form.FormPanel;
import platform.gwt.form.client.navigator.NavigatorPanel;
import platform.gwt.sgwtbase.client.ui.ToolStripPanel;

public class MainFrame extends VLayout implements EntryPoint {
    private static final MainFrameMessages messages = MainFrameMessages.Instance.get();

    private FormPanel formPanel;
    private NavigatorPanel navigatorPanel;

    public void onModuleLoad() {
        GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
            @Override
            public void onUncaughtException(Throwable t) {
                GWT.log("Необработанная ошибка в GWT: ", t);
            }
        });

        setWidth100();
        setHeight100();

        navigatorPanel = new NavigatorPanel(this);

        HLayout main = new HLayout(20);
        main.addMember(navigatorPanel);

        formPanel = new FormPanel();
        main.addMember(formPanel);

        addMember(new ToolStripPanel(messages.title()));
        addMember(main);

        draw();

        GwtClientUtils.removeLoaderFromHostedPage();
    }
}
