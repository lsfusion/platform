package lsfusion.gwt.client.form.design.view.flex;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.CaptionPanel;

public class FlexCaptionPanel extends CaptionPanel {
    public FlexCaptionPanel(String title, Widget content) {
        super(title, content);
    }

    public void setCaption(String title) {
        legend.setText(EscapeUtils.unicodeEscape(title));
    }

    @Override
    public Dimension getMaxPreferredSize() {
        Dimension sz = GwtClientUtils.calculateMaxPreferredSize(content);
        sz.height += legend.getOffsetHeight() + 5;
        sz.width += 5;
        return sz;
    }
}
