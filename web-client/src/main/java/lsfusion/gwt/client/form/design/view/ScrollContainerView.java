package lsfusion.gwt.client.form.design.view;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;

public class ScrollContainerView extends GAbstractContainerView {

    private final FlexPanel scrollPanel;
    private final boolean vertical = true;

    protected final Widget view;

    public ScrollContainerView(GContainer container) {
        super(container);

        assert container.isScroll();

        scrollPanel = new FlexPanel(vertical);
        view = initBorder(scrollPanel);
        view.getElement().getStyle().setOverflowY(Style.Overflow.AUTO);
        view.getElement().getStyle().setOverflowX(Style.Overflow.AUTO);
    }

    @Override
    protected void addImpl(int index, GComponent child, Widget view) {
        // it's a very odd hack to enable "opposite" scrolling, that will work really unstable
        // it's needed since stretch always gives 100% width / height even if the component contents is bigger
        if(child.getAlignment() == GFlexAlignment.STRETCH) {
            view.getElement().getStyle().setOverflowY(Style.Overflow.VISIBLE); // without this horizontal scroller is shown for view and not element (it's also really odd)
            view.getElement().getStyle().setOverflowX(Style.Overflow.VISIBLE);
        }

        add(scrollPanel, view, child, 0);
    }

    @Override
    protected void removeImpl(int index, GComponent child, Widget view) {
        scrollPanel.remove(view);
    }

    @Override
    public Widget getView() {
        return view;
    }
}