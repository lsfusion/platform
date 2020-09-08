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
        if(child.getFlex() > 0 && hasSeveralFlexes && view instanceof TabbedContainerView.Panel)
            ((TabbedContainerView.Panel)view).setBeforeSelectionHandler(tabIndex -> {
                if(tabIndex > 0)
                    panel.fixFlexBasis((TabbedContainerView.Panel)view);
            });

        child.installMargins(view);

//        if(child.hasMargins()) {
//            FlexPanel proxyPanel = new FlexPanel(container.isLinearVertical());
//            proxyPanel.addFill(view);
//            view = proxyPanel;
//        }
        add(panel, view, child, index);
    }

    @Override
    protected void removeImpl(int index, GComponent child, Widget view) {
//        if (child.hasMargins()) {
//            view.getParent().removeFromParent();
//        }
        view.removeFromParent();
    }

    @Override
    public Widget getView() {
        return view;
    }
}
