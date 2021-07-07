package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.base.GwtClientUtils;

public class CaptionPanel extends FlexPanel {
    protected final Label legend;

    public CaptionPanel(String caption, boolean vertical) {
        super(vertical);

        legend = new Label(EscapeUtils.unicodeEscape(caption));
        legend.setStyleName("captionPanelLegend");
        add(legend);

        setStyleName("captionPanel");
    }
    public CaptionPanel(String caption, Widget content) {
        this(caption, true);

        addFillFlex(content, null);
    }

    @Override
    public Dimension getMaxPreferredSize() {
        return adjustMaxPreferredSize(GwtClientUtils.calculateMaxPreferredSize(getWidget(1))); // assuming that there are only 2 widgets, and the second is the main widget
    }

    public Dimension adjustMaxPreferredSize(Dimension dimension) {
        return new Dimension(dimension.width + 5, dimension.height + legend.getOffsetHeight() + 5);
    }

    public void setCaption(String title) {
        legend.setText(EscapeUtils.unicodeEscape(title));
    }
}
