package lsfusion.gwt.client.form.ui.layout.flex;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.ui.GCaptionPanel;

public class FlexCaptionPanel extends GCaptionPanel {
    public FlexCaptionPanel(String title, Widget content) {
        super(title, content);
    }

    @Override
    public Dimension getMaxPreferredSize() {
        Dimension sz = GwtClientUtils.calculateMaxPreferredSize(content);
        sz.height += legend.getOffsetHeight() + 5;
        sz.width += 5;
        return sz;
    }
}
