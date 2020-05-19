package lsfusion.gwt.client.form.design.view;

import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.flex.FlexSplitPanel;

import java.util.Map;

public class SplitContainerView<P extends Panel> extends GAbstractContainerView {

    private final SplitPanelBase splitPane;

    private final Widget view;

    public SplitContainerView(GContainer container) {
        super(container);

        assert container.isSplit();

        splitPane = new FlexSplitPanel(container.isSplitVertical());

        view = initBorder(splitPane.asWidget());
//
//        view.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);
    }

    @Override
    protected void addImpl(int index, GComponent child, Widget view) {
        if (container.children.get(0) == child) {
            splitPane.addFirstWidget(child, view);
        } else if (container.children.get(1) == child) {
            splitPane.addSecondWidget(child, view);
        }
    }

    @Override
    protected void removeImpl(int index, GComponent child, Widget view) {
        splitPane.remove(view);
    }

    @Override
    public Widget getView() {
        return view;
    }

    public void updateLayout() {
        super.updateLayout();
        splitPane.update();
    }

    @Override
    public Dimension getMaxPreferredSize(Map<GContainer, GAbstractContainerView> containerViews) {
        Dimension pref = super.getMaxPreferredSize(containerViews);
        
        if (container.isSplitVertical()) {
            pref.height += splitPane.getSplitterSize();
        } else {
            pref.width += splitPane.getSplitterSize();
        }

        return pref;
    }
}
