package lsfusion.gwt.form.client.form.ui.layout.flex;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.base.client.ui.GFlexAlignment;
import lsfusion.gwt.form.client.form.ui.layout.SplitPanelBase;

public class FlexSplitPanel extends SplitPanelBase<FlexPanel> {

    public FlexSplitPanel(boolean vertical) {
        super(vertical, new FlexPanel(vertical));
    }

    @Override
    protected void addSplitterImpl(Splitter splitter) {
        panel.add(splitter, GFlexAlignment.STRETCH);
    }

    @Override
    protected void addFirstWidgetImpl(Widget widget) {
        panel.add(firstWidget, 0, GFlexAlignment.STRETCH, flex1, "0px");
    }

    @Override
    protected void addSecondWidgetImpl(Widget secondWidget) {
        int index = firstWidget == null ? 1 : 2;
        panel.add(secondWidget, index, GFlexAlignment.STRETCH, flex2, "0px");
    }

    @Override
    protected void setChildrenRatio(double ratio) {
        double f1 = flexSum * ratio;
        if (firstWidget != null) {
            panel.setChildFlex(firstWidget, f1, "0px");
        }
        if (secondWidget != null) {
            double f2 = flexSum - f1;
            panel.setChildFlex(secondWidget, f2, "0px");
        }
    }
}
