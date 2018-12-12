package lsfusion.gwt.client.form.ui.layout.flex;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.ui.FlexPanel;
import lsfusion.gwt.client.form.ui.layout.SplitContainerView;
import lsfusion.gwt.client.form.ui.layout.SplitPanelBase;
import lsfusion.gwt.shared.view.GContainer;

public class FlexSplitContainerView extends SplitContainerView<FlexPanel> {
    public FlexSplitContainerView(GContainer container) {
        super(container);
    }

    @Override
    protected SplitPanelBase<FlexPanel> createSplitPanel(boolean vertical) {
//        return new FlexSplitPanel_IEBug(vertical);
        return new FlexSplitPanel(vertical);
    }

    @Override
    protected Widget wrapWithCaption(FlexPanel panel) {
        return wrapWithFlexCaption(panel);
    }
}
