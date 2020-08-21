package lsfusion.gwt.client.form.design.view.flex;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.GAbstractContainerView;
import lsfusion.gwt.client.form.design.view.TabbedContainerView;

public class LinearContainerView extends GAbstractContainerView {
    private final FlexPanel panel;

    private final Widget view;

    private final boolean hasSeveralFlexes;

    public LinearContainerView(GContainer container) {
        super(container);

        assert container.isLinear();

        panel = new FlexPanel(container.isVertical(), container.getFlexJustify());
        panel.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);

        hasSeveralFlexes = container.getFlexCount() > 1;

        view = initBorder(panel);
    }

    @Override
    protected void addImpl(int index, GComponent child, final Widget view) {
        boolean vertical = container.isLinearVertical(); // assert isLinear
        if (child.hasMargins()) {
            FlexPanel proxyPanel = new FlexPanel(vertical);
            add(proxyPanel, view, 0, child.getAlignment(), child.getFlex(), child, vertical);
            proxyPanel.getElement().getStyle().setOverflow(Style.Overflow.VISIBLE);
            child.installPaddings(proxyPanel);

            panel.add(proxyPanel, index, child.getAlignment(), child.getFlex() > 0 ? 1 : 0);
        } else {
            add(panel, view, index, child.getAlignment(), child.getFlex(), child, vertical);
        }

        if(child.getFlex() > 0 && hasSeveralFlexes && view instanceof TabbedContainerView.Panel)
            ((TabbedContainerView.Panel)view).setBeforeSelectionHandler(tabIndex -> {
                if(tabIndex > 0)
                    panel.fixFlexBasis((TabbedContainerView.Panel)view);
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
}
