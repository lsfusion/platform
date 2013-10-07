package lsfusion.gwt.form.client.form.ui.layout.flex;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.form.client.form.ui.layout.SplitPanelBase;

public class FlexSplitPanel extends SplitPanelBase<FlexPanel> {

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
    protected void addFirstWidgetImpl(Widget widget) {
        Style style = widget.getElement().getStyle();
        style.setPosition(Style.Position.ABSOLUTE);
        style.setTop(0, Style.Unit.PX);
        style.setLeft(0, Style.Unit.PX);
        if (vertical) {
            style.setRight(0, Style.Unit.PX);
        } else {
            style.setBottom(0, Style.Unit.PX);
        }
        panel.add(firstWidget, 0);
    }

    @Override
    protected void addSecondWidgetImpl(Widget widget) {
        Style style = widget.getElement().getStyle();
        style.setPosition(Style.Position.ABSOLUTE);
        style.setBottom(0, Style.Unit.PX);
        style.setRight(0, Style.Unit.PX);
        if (vertical) {
            style.setLeft(0, Style.Unit.PX);
        } else {
            style.setTop(0, Style.Unit.PX);
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
            if (vertical) {
                firstWidget.getElement().getStyle().setHeight(size1, Style.Unit.PX);
            } else {
                firstWidget.getElement().getStyle().setWidth(size1, Style.Unit.PX);
            }
        }
        if (secondWidget != null) {
            int size2 = Math.max(0, availableSize - size1);
            if (vertical) {
                secondWidget.getElement().getStyle().setHeight(size2, Style.Unit.PX);
            } else {
                secondWidget.getElement().getStyle().setWidth(size2, Style.Unit.PX);
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
