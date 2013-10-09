package lsfusion.gwt.form.client.form.ui.layout.flex;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.form.client.form.ui.layout.SplitPanelBase;
import lsfusion.gwt.form.shared.view.GComponent;

public class FlexSplitPanel extends SplitPanelBase<FlexPanel> {

    private GComponent firstChild;
    private GComponent secondChild;

    public FlexSplitPanel(boolean vertical) {
        super(vertical, new Panel());
        ((Panel)panel).setSplitPanel(this);
        panel.getElement().getStyle().setPosition(Style.Position.RELATIVE);
    }

    @Override
    protected void addSplitterImpl(Splitter splitter) {
        Style splitterStyle = splitter.getElement().getStyle();
        splitterStyle.setPosition(Style.Position.ABSOLUTE);
        if (vertical) {
            splitterStyle.setLeft(0, Style.Unit.PX);
            splitterStyle.setRight(0, Style.Unit.PX);
        } else {
            splitterStyle.setTop(0, Style.Unit.PX);
            splitterStyle.setBottom(0, Style.Unit.PX);
        }
        panel.add(splitter);
    }

    @Override
    protected void addFirstWidgetImpl(GComponent child, Widget widget) {
        firstChild = child;

        Style style = widget.getElement().getStyle();
        style.setPosition(Style.Position.ABSOLUTE);
        style.setTop(child.marginTop, Style.Unit.PX);
        style.setLeft(child.marginLeft, Style.Unit.PX);
        if (vertical) {
            style.setRight(child.marginRight, Style.Unit.PX);
        } else {
            style.setBottom(child.marginBottom, Style.Unit.PX);
        }
        panel.add(firstWidget, 0);
    }

    @Override
    protected void addSecondWidgetImpl(GComponent child, Widget widget) {
        secondChild = child;

        Style style = widget.getElement().getStyle();
        style.setPosition(Style.Position.ABSOLUTE);
        style.setBottom(child.marginBottom, Style.Unit.PX);
        style.setRight(child.marginRight, Style.Unit.PX);
        if (vertical) {
            style.setLeft(child.marginLeft, Style.Unit.PX);
        } else {
            style.setTop(child.marginTop, Style.Unit.PX);
        }

        int index = firstWidget == null ? 1 : 2;
        panel.add(widget, index);
    }

    @Override
    protected void setChildrenRatio(double ratio) {
        int availableSize = getAvailableSize();

        int size1 = (int) Math.max(0, availableSize * ratio);

        if (vertical) {
            splitter.getElement().getStyle().setTop(size1, Style.Unit.PX);
        } else {
            splitter.getElement().getStyle().setLeft(size1, Style.Unit.PX);
        }

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

        private void setSplitPanel(FlexSplitPanel splitPanel) {
            this.splitPanel = splitPanel;
        }

        @Override
        public void onResize() {
            splitPanel.update();
            super.onResize();
        }
    }
}
