package lsfusion.gwt.form.client.log;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ScrollPanel;

public class GLogPanel extends ScrollPanel {
    private HTMLPanel panel;

    public GLogPanel() {
        super();
        panel = new HTMLPanel("");
        addStyleName("logPanel");
        add(panel);
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
    }
}
