package lsfusion.gwt.client.base.log;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import lsfusion.gwt.client.base.view.RecentlyEventClassHandler;
import lsfusion.gwt.client.navigator.view.NavigatorPanel;

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

    public void printMessage(String message) {
        commitMessage(new HTML(message));
    }

    public void printError(String errorMessage) {
        HTML errorLabel = new HTML(errorMessage);
        errorLabel.addStyleName("errorLogMessage");
        commitMessage(errorLabel);
    }

    private void commitMessage(HTML message) {
        logPanel.add(message);
        Element logElement = logPanel.getElement();
        logElement.setScrollTop(logElement.getScrollHeight() - logElement.getClientHeight());
        recentlySelected.onEvent();
    }
}
