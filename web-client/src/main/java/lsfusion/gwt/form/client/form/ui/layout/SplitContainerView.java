package lsfusion.gwt.form.client.form.ui.layout;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.Dimension;
import lsfusion.gwt.form.shared.view.GComponent;
import lsfusion.gwt.form.shared.view.GContainer;

import java.util.Map;

public abstract class SplitContainerView<P extends Panel> extends GAbstractContainerView {

    private final SplitPanelBase<P> splitPane;

    private final Widget view;

    public SplitContainerView(GContainer container) {
        super(container);

        assert container.isSplit();

        splitPane = createSplitPanel(container.isVerticalSplit());

        view = wrawpWithCaption(splitPane.asWidget());

        view.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);
    }

    protected abstract SplitPanelBase<P> createSplitPanel(boolean vertical);
    protected abstract Widget wrawpWithCaption(P panel);

    @Override
    protected void addImpl(int index, GComponent child, Widget view) {
        if (container.children.get(0) == child) {
            splitPane.addFirstWidget(child, view, child.flex);
        } else if (container.children.get(1) == child) {
            splitPane.addSecondWidget(child, view, child.flex);
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
        splitPane.update();
    }

    @Override
    public Dimension getPreferredSize(Map<GContainer, GAbstractContainerView> containerViews) {
        Dimension pref = getChildrenStackSize(containerViews, container.isVerticalSplit());

        if (container.isVerticalSplit()) {
            pref.height += splitPane.getSplitterSize();
        } else {
            pref.width += splitPane.getSplitterSize();
        }

        return pref;
    }
}
