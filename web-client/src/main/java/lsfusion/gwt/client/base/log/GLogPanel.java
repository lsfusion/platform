package lsfusion.gwt.client.base.log;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import lsfusion.gwt.client.base.view.RecentlyEventClassHandler;

public class GLogPanel extends ScrollPanel {
    private HTMLPanel panel;

    RecentlyEventClassHandler recentlySelected;

    public GLogPanel() {
        super();
        panel = new HTMLPanel("");
        addStyleName("logPanel");
        add(panel);

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
        panel.add(message);
        scrollToBottom();
        recentlySelected.onEvent();
    }
}
