package lsfusion.gwt.form.client.form.ui.layout.flex;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.Dimension;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.form.client.form.ui.layout.GAbstractContainerView;
import lsfusion.gwt.form.shared.view.GComponent;
import lsfusion.gwt.form.shared.view.GContainer;

import java.util.Map;

public class FlexLinearContainerView extends GAbstractContainerView {
    private final FlexPanel panel;

    private final Widget view;

    public FlexLinearContainerView(GContainer container) {
        super(container);

        assert container.isLinear();

        panel = new FlexPanel(container.isVertical(), container.getFlexJustify());
        panel.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);

        view = wrapWithFlexCaption(panel);
    }

    @Override
    protected void addImpl(int index, GComponent child, Widget view) {
        if (child.hasMargins()) {
            FlexPanel proxyPanel = new FlexPanel(container.isVertical());
            proxyPanel.getElement().getStyle().setOverflow(Style.Overflow.VISIBLE);
            child.installPaddings(proxyPanel);

            proxyPanel.add(view, child.alignment, child.flex > 0 ? 1 : 0);

            panel.add(proxyPanel, index, child.alignment, child.flex);
        } else {
            panel.add(view, index, child.alignment, child.flex);
        }
    }

    @Override
    protected void removeImpl(int index, GComponent child, Widget view) {
        if (child.hasMargins()) {
            //удаляем ProxyPanel
            view.getParent().removeFromParent();
        }
        view.removeFromParent();
    }

    @Override
    public Widget getView() {
        return view;
    }

    @Override
    public Dimension getPreferredSize(Map<GContainer, GAbstractContainerView> containerViews) {
        return getChildrenStackSize(containerViews, container.isVertical());
    }
}
