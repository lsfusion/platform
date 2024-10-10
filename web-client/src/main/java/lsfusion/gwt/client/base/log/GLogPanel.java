package lsfusion.gwt.client.base.log;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.RecentlyEventClassHandler;
import lsfusion.gwt.client.navigator.view.NavigatorPanel;
import lsfusion.gwt.client.view.MainFrame;

import java.util.Date;

public class GLogPanel extends NavigatorPanel {

    private FlexPanel logPanel;

    private RecentlyEventClassHandler recentlySelected;

    public GLogPanel() {
        super(true);

        logPanel = new FlexPanel(true);
        GwtClientUtils.addClassName(logPanel, "nav-log-panel");

        panel.add(logPanel);

        recentlySelected = new RecentlyEventClassHandler(panel, true, "parent-was-selected-recently", 2000);
    }

    public void printMessage(Widget message, String caption, boolean failed) {
        String messageClass = failed ? "errorLogMessage" : "successLogMessage";

        HTML messageDate = new HTML(DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_TIME_MEDIUM).format(new Date(System.currentTimeMillis())) + " " + caption);
        GwtClientUtils.addClassName(message, messageClass);
        logPanel.add(message,  0, GFlexAlignment.STRETCH);
        GwtClientUtils.addClassName(messageDate, messageClass);
        logPanel.add(messageDate, 0, GFlexAlignment.STRETCH);

        if (MainFrame.enableShowingRecentlyLogMessages) {
            recentlySelected.onEvent();
        }
    }
}
