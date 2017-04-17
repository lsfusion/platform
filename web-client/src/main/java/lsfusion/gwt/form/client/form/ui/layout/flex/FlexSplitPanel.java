package lsfusion.gwt.form.client.form.ui.layout.flex;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.base.client.ui.GFlexAlignment;
import lsfusion.gwt.form.client.form.ui.layout.SplitPanelBase;
import lsfusion.gwt.form.shared.view.GComponent;

public class FlexSplitPanel extends SplitPanelBase<FlexPanel> {

    private GComponent firstChild;
    private GComponent secondChild;

    public FlexSplitPanel(boolean vertical) {
        super(vertical, new Panel(vertical));
        ((Panel)panel).setSplitPanel(this);
    }

    @Override
    protected void addSplitterImpl(Splitter splitter) {
        panel.add(splitter, GFlexAlignment.STRETCH);
    }

    @Override
    protected void addFirstWidgetImpl(GComponent child, Widget widget) {
        firstChild = child;
        panel.add(firstWidget, 0, GFlexAlignment.STRETCH, 0);
        Style style = widget.getElement().getStyle();
        style.setOverflowY(vertical ? Style.Overflow.AUTO : Style.Overflow.HIDDEN);
        style.setOverflowX(vertical ? Style.Overflow.HIDDEN : Style.Overflow.AUTO);
    }

    @Override
    protected void addSecondWidgetImpl(GComponent child, Widget widget) {
        secondChild = child;
        panel.add(widget, firstWidget == null ? 1 : 2, GFlexAlignment.STRETCH, 0);
        Style style = widget.getElement().getStyle();
        style.setOverflowY(vertical ? Style.Overflow.AUTO : Style.Overflow.HIDDEN);
        style.setOverflowX(vertical ? Style.Overflow.HIDDEN : Style.Overflow.AUTO);
    }

    @Override
    protected void setChildrenRatio(double ratio) {
        int availableSize = getAvailableSize();

        int size1 = (int) Math.max(0, availableSize * ratio);

        if (firstWidget != null) {
            int sizeToSet1 = Math.max(0, size1 - firstChild.getMargins(vertical));
            if (vertical) {
                firstWidget.getElement().getStyle().setHeight(sizeToSet1, Style.Unit.PX);
            } else {
                firstWidget.getElement().getStyle().setWidth(sizeToSet1, Style.Unit.PX);
            }
        }
        if (secondWidget != null) {
            int sizeToSet2 = Math.max(0, availableSize - size1 - secondChild.getMargins(vertical));
            if (vertical) {
                secondWidget.getElement().getStyle().setHeight(sizeToSet2, Style.Unit.PX);
            } else {
                secondWidget.getElement().getStyle().setWidth(sizeToSet2, Style.Unit.PX);
            }
        }
    }

    private static class Panel extends FlexPanel {
        FlexSplitPanel splitPanel;
        
        public Panel(boolean vertical) {
            super(vertical);
        }

        private void setSplitPanel(FlexSplitPanel splitPanel) {
            this.splitPanel = splitPanel;
        }

        @Override
        public double getFlexShrink() {
            return isVertical() ? 1 : 0;
        }

        @Override
        public void onResize() {
            splitPanel.update();
            super.onResize();
        }
    }
}
