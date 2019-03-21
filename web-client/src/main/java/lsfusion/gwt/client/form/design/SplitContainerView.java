package lsfusion.gwt.client.form.design;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.shared.form.design.GComponent;
import lsfusion.gwt.shared.form.design.GContainer;

import java.util.Map;

public abstract class SplitContainerView<P extends Panel> extends GAbstractContainerView {

    private final SplitPanelBase<P> splitPane;

    private final Widget view;

    public SplitContainerView(GContainer container) {
        super(container);

        assert container.isSplit();

        splitPane = createSplitPanel(container.isSplitVertical());

        view = wrapWithCaption(splitPane.asWidget());

        view.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);
    }

    protected abstract SplitPanelBase<P> createSplitPanel(boolean vertical);
    protected abstract Widget wrapWithCaption(P panel);

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
