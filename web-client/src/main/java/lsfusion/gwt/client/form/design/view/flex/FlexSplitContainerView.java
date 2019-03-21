package lsfusion.gwt.client.form.design.view.flex;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.SplitContainerView;
import lsfusion.gwt.client.form.design.view.SplitPanelBase;

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
