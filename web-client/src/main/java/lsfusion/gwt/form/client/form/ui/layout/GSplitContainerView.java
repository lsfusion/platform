package lsfusion.gwt.form.client.form.ui.layout;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.form.shared.view.GComponent;
import lsfusion.gwt.form.shared.view.GContainer;

public class GSplitContainerView extends GAbstractContainerView {

    private final GSplitPane splitPane;

    private final Widget view;

    public GSplitContainerView(GContainer container) {
        super(container);

        assert container.isSplit();

        splitPane = new GSplitPane(container.isVerticalSplit());

        view = wrapWithCaption(splitPane.getComponent());
        view.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);
    }

    @Override
    protected void addImpl(int index, GComponent child, Widget view) {
        if (container.children.get(0) == child) {
            splitPane.addFirstWidget(view, child.flex);
        } else if (container.children.get(1) == child) {
            splitPane.addSecondWidget(view, child.flex);
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
}
