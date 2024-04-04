package lsfusion.gwt.client.base.log;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.base.view.RecentlyEventClassHandler;
import lsfusion.gwt.client.navigator.view.NavigatorPanel;
import lsfusion.gwt.client.view.MainFrame;

import java.util.Date;

public class GLogPanel extends NavigatorPanel {

    private HTMLPanel logPanel;

    private RecentlyEventClassHandler recentlySelected;

    public GLogPanel() {
        super(false);

        logPanel = new HTMLPanel("");
        logPanel.addStyleName("nav-log-panel");

        panel.add(logPanel);

        recentlySelected = new RecentlyEventClassHandler(panel, true, "parent-was-selected-recently", 2000);
    }

    public void printMessage(Widget message) {
        commitMessage(message, "successLogMessage");
    }

    public void printError(Widget errorMessage) {
        commitMessage(errorMessage, "errorLogMessage");
    }

    private void commitMessage(Widget message, String messageClass) {
        HTML messageDate = new HTML("--- " + DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_TIME_MEDIUM).format(new Date(System.currentTimeMillis())) + " ---<br/>");
        messageDate.addStyleName(messageClass);
        logPanel.add(messageDate);
        message.addStyleName(messageClass);
        logPanel.add(message);

        Element logElement = logPanel.getElement();
        logElement.setScrollTop(logElement.getScrollHeight() - logElement.getClientHeight());

        if (!MainFrame.disableShowingRecentlyLogMessages) {
            recentlySelected.onEvent();
        }
    }
}
