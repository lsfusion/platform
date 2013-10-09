package lsfusion.gwt.form.client.form.ui.layout.flex;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.form.client.form.ui.layout.SplitContainerView;
import lsfusion.gwt.form.client.form.ui.layout.SplitPanelBase;
import lsfusion.gwt.form.shared.view.GContainer;

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
    protected Widget wrawpWithCaption(FlexPanel panel) {
        return wrapWithFlexCaption(panel);
    }
}
