package lsfusion.gwt.client.base.log;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
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
        logPanel.addStyleName("nav-log-panel");

        panel.add(logPanel);

        recentlySelected = new RecentlyEventClassHandler(panel, true, "parent-was-selected-recently", 2000);
    }

    public void printMessage(Widget message, String caption, boolean failed) {
        String messageClass = failed ? "errorLogMessage" : "successLogMessage";

        HTML messageDate = new HTML(DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_TIME_MEDIUM).format(new Date(System.currentTimeMillis())) + " " + caption);
        message.addStyleName(messageClass);
        logPanel.add(message,  0, GFlexAlignment.STRETCH);
        messageDate.addStyleName(messageClass);
        logPanel.add(messageDate, 0, GFlexAlignment.STRETCH);

        if (MainFrame.enableShowingRecentlyLogMessages) {
            recentlySelected.onEvent();
        }
    }
}
