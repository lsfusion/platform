package lsfusion.gwt.form.client.form.ui.layout.flex;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.Dimension;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.form.client.form.ui.layout.BeforeSelectionTabHandler;
import lsfusion.gwt.form.client.form.ui.layout.GAbstractContainerView;
import lsfusion.gwt.form.client.form.ui.layout.TabbedContainerView;
import lsfusion.gwt.form.client.form.ui.layout.TabbedPanelBase;
import lsfusion.gwt.form.shared.view.GComponent;
import lsfusion.gwt.form.shared.view.GContainer;

import java.util.Map;

public class FlexLinearContainerView extends GAbstractContainerView {
    private final FlexPanel panel;

    private final Widget view;

    private final boolean hasSeveralFlexes;

    public FlexLinearContainerView(GContainer container) {
        super(container);

        assert container.isLinear();

        panel = new FlexPanel(container.isVertical(), container.getFlexJustify());
        panel.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);

        view = wrapWithFlexCaption(panel);

        hasSeveralFlexes = container.getFlexCount() > 1;
    }

    @Override
    protected void addImpl(int index, GComponent child, final Widget view) {
        boolean vertical = container.isVertical();
        if (child.hasMargins()) {
            FlexPanel proxyPanel = new FlexPanel(vertical);
            proxyPanel.getElement().getStyle().setOverflow(Style.Overflow.VISIBLE);
            child.installPaddings(proxyPanel);

            proxyPanel.add(view, child.alignment, child.flex > 0 ? 1 : 0);

            add(panel, proxyPanel, index, child.alignment, child.flex, child, vertical);
        } else {
            add(panel, view, index, child.alignment, child.flex, child, vertical);
        }

        if(child.flex > 0 && hasSeveralFlexes && view instanceof TabbedPanelBase)
            ((TabbedPanelBase)view).addBeforeSelectionTabHandler(new BeforeSelectionTabHandler() {
                @Override
                public void onBeforeSelection(int tabIndex) {
                    if(tabIndex > 0)
                        panel.fixFlexBasis(view);
                }
            });
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
