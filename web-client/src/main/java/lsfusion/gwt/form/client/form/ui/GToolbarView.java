package lsfusion.gwt.form.client.form.ui;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.Dimension;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.form.shared.view.GToolbar;

public class GToolbarView extends FlexPanel {
    private final GToolbar toolbar;

    public GToolbarView(GToolbar toolbar) {
        this.toolbar = toolbar;
        toolbar.installMargins(this);
    }

    public void addTool(Widget tool) {
        add(tool);
    }

    @Override
    public Dimension getPreferredSize() {
        return toolbar.getOffsetSize(this);
    }
}
