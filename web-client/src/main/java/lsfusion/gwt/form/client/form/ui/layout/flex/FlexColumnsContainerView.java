package lsfusion.gwt.form.client.form.ui.layout.flex;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.form.client.form.ui.layout.ColumnsContainerView;
import lsfusion.gwt.form.shared.view.GContainer;

public class FlexColumnsContainerView extends ColumnsContainerView<FlexPanel> {
    public FlexColumnsContainerView(GContainer container) {
        super(container);
    }

    protected FlexPanel createHorizontalPanel() {
        return new FlexPanel();
    }

    @Override
    protected Widget wrapWithCaption(FlexPanel panel) {
        return wrapWithCaptionAndSetMargins(panel);
    }
}
