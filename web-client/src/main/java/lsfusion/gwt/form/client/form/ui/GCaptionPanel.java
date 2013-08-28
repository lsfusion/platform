package lsfusion.gwt.form.client.form.ui;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.EscapeUtils;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.base.client.ui.GFlexAlignment;

public class GCaptionPanel extends FlexPanel {
    public GCaptionPanel(String title, Widget content) {
        super(true);

        Label legend = new Label(EscapeUtils.unicodeEscape(title));

        FlexPanel innerPanel = new FlexPanel(true);
        innerPanel.add(legend);
        innerPanel.add(content, GFlexAlignment.STRETCH, 1, "auto");

        add(innerPanel, GFlexAlignment.STRETCH, 1, "auto");

        setStyleName("captionPanel");
        innerPanel.setStyleName("captionPanelContainer");
        legend.setStyleName("captionPanelLegend");

        innerPanel.getElement().getStyle().clearOverflow();
    }
}
