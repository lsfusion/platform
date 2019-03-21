package lsfusion.gwt.client.form.design.view.flex;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.form.design.view.BeforeSelectionTabHandler;
import lsfusion.gwt.client.form.design.view.GAbstractContainerView;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;

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
        boolean vertical = container.isLinearVertical(); // assert isLinear
        if (child.hasMargins()) {
            FlexPanel proxyPanel = new FlexPanel(vertical);
            proxyPanel.getElement().getStyle().setOverflow(Style.Overflow.VISIBLE);
            child.installPaddings(proxyPanel);

            proxyPanel.add(view, child.getAlignment(), child.getFlex() > 0 ? 1 : 0);

            add(panel, proxyPanel, index, child.getAlignment(), child.getFlex(), child, vertical);
        } else {
            add(panel, view, index, child.getAlignment(), child.getFlex(), child, vertical);
        }

        if(child.getFlex() > 0 && hasSeveralFlexes && view instanceof FlexTabbedContainerView.Panel)
            ((FlexTabbedContainerView.Panel)view).addBeforeSelectionTabHandler(new BeforeSelectionTabHandler() {
                @Override
                public void onBeforeSelection(int tabIndex) {
                    if(tabIndex > 0)
                        panel.fixFlexBasis((FlexTabbedContainerView.Panel)view);
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
}
