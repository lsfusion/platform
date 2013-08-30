package lsfusion.gwt.form.client.form.ui;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.Dimension;
import lsfusion.gwt.base.client.EscapeUtils;
import lsfusion.gwt.base.client.GwtClientUtils;
import lsfusion.gwt.base.client.ui.FlexPanel;

public class GCaptionPanel extends FlexPanel {
    private final Widget content;
    private final Label legend;

    public GCaptionPanel(String caption, Widget content) {
        super(true);

        this.content = content;

        legend = new Label(EscapeUtils.unicodeEscape(caption));

        FlexPanel innerPanel = new FlexPanel(true);
        innerPanel.add(legend);
        innerPanel.addFill(content);

        addFill(innerPanel);

        setStyleName("captionPanel");
        innerPanel.setStyleName("captionPanelContainer");
        legend.setStyleName("captionPanelLegend");

        innerPanel.getElement().getStyle().clearOverflow();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension sz = GwtClientUtils.maybeGetPreferredSize(content);
        sz.height += legend.getOffsetHeight() + 5;
        sz.width += 5;
        return sz;
    }
}
