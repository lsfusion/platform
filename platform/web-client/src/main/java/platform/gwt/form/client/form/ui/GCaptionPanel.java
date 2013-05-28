package platform.gwt.form.client.form.ui;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.base.client.EscapeUtils;
import platform.gwt.base.client.ui.ResizableVerticalPanel;

public class GCaptionPanel extends ResizableVerticalPanel {
    public GCaptionPanel(String title, Widget content) {
        setStyleName("captionPanel");
        setSize("100%", "100%");

        ResizableVerticalPanel container = new ResizableVerticalPanel();
        container.setSize("100%", "100%");
        container.setStyleName("captionPanelContainer");

        Label legend = new Label(EscapeUtils.unicodeEscape(title));
        legend.setStyleName("captionPanelLegend");

        container.add(legend);
        container.add(content);

        container.setCellHeight(content, "100%");
        container.setCellWidth(content, "100%");

        add(container);
        setCellHeight(container, "100%");
    }
}
