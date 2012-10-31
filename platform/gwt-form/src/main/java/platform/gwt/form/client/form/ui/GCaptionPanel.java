package platform.gwt.form.client.form.ui;

import com.google.gwt.user.client.ui.*;

public class GCaptionPanel extends VerticalPanel {
    public GCaptionPanel(String title, Widget content) {
        setStyleName("captionPanel");
        setSize("100%", "100%");

        VerticalPanel container = new VerticalPanel();
        container.setSize("100%", "100%");
        container.setStyleName("captionPanelContainer");

        HTMLPanel legend = new HTMLPanel(title);
        legend.setStyleName("captionPanelLegend");

        container.add(legend);
        container.add(content);

        container.setCellHeight(content, "100%");
        container.setCellWidth(content, "100%");

        add(container);
    }
}
