package lsfusion.gwt.client.form.design.view.flex;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.form.design.view.ColumnsContainerView;
import lsfusion.gwt.client.form.design.GContainer;

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
