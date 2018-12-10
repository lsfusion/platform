package lsfusion.gwt.client.form.form.ui.layout.flex;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.ui.FlexPanel;
import lsfusion.gwt.client.form.form.ui.layout.ColumnsContainerView;
import lsfusion.gwt.shared.form.view.GContainer;

public class FlexColumnsContainerView extends ColumnsContainerView<FlexPanel> {
    public FlexColumnsContainerView(GContainer container) {
        super(container);
    }

    protected FlexPanel createHorizontalPanel() {
        return new FlexPanel();
    }

    @Override
    protected Widget wrapWithCaption(FlexPanel panel) {
        return wrapWithFlexCaption(panel);
    }
}
