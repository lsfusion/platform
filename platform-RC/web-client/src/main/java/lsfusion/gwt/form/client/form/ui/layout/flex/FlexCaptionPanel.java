package lsfusion.gwt.form.client.form.ui.layout.flex;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.Dimension;
import lsfusion.gwt.base.client.GwtClientUtils;
import lsfusion.gwt.form.client.form.ui.GCaptionPanel;

public class FlexCaptionPanel extends GCaptionPanel {
    public FlexCaptionPanel(String title, Widget content) {
        super(title, content);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension sz = GwtClientUtils.calculatePreferredSize(content);
        sz.height += legend.getOffsetHeight() + 5;
        sz.width += 5;
        return sz;
    }
}
