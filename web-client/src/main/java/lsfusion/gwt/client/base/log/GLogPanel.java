package lsfusion.gwt.client.base.log;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.RecentlyEventClassHandler;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.view.MainFrame;

import java.util.Date;

// the standard panel: the messages one under another, newest first, each with the caption and the time it arrived, and
// the platform's pin button under them
public class GLogPanel extends LogPanel {

    private FlexPanel logPanel;

    private RecentlyEventClassHandler recentlySelected;

    public GLogPanel(Runnable togglePinMode) {
        logPanel = new FlexPanel(true);
        GwtClientUtils.addClassName(logPanel, "nav-log-panel");

        panel.add(logPanel);

        GToolbarButton pinButton = new GToolbarButton(StaticImage.PIN, ClientMessages.Instance.get().logPanelPinModeToggle()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> togglePinMode.run();
            }
        };
        GwtClientUtils.addClassName(pinButton, "nav-log-pin");
        panel.add(pinButton);

        recentlySelected = new RecentlyEventClassHandler(panel, true, "parent-was-selected-recently", 2000);
    }

    @Override
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
