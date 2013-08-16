package lsfusion.gwt.form.client.form.ui;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.EscapeUtils;
import lsfusion.gwt.base.client.ui.GFlexAlignment;
import lsfusion.gwt.base.client.ui.FlexPanel;

public class GCaptionPanel extends FlexPanel {
    public GCaptionPanel(String title, Widget content) {
        super(true);

        setStyleName("captionPanelContainer");

        Label legend = new Label(EscapeUtils.unicodeEscape(title));
        legend.setStyleName("captionPanelLegend");

        getElement().getStyle().setOverflow(Style.Overflow.VISIBLE);

        add(legend);
        add(content, GFlexAlignment.STRETCH, 1, "auto");
    }
}
