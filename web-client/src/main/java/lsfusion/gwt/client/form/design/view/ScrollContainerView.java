package lsfusion.gwt.client.form.design.view;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
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
        scrollPanel.getElement().getStyle().setOverflowY(Style.Overflow.AUTO);
        scrollPanel.getElement().getStyle().setOverflowX(Style.Overflow.AUTO);

        view = initBorder(scrollPanel);
    }

    @Override
    protected void addImpl(int index, GComponent child, Widget view) {
        assert child.getFlex() == 1 && child.getAlignment() == GFlexAlignment.STRETCH; // временные assert'ы чтобы проверить обратную совместимость
        view.getElement().getStyle().setOverflowY(Style.Overflow.VISIBLE);
        view.getElement().getStyle().setOverflowX(Style.Overflow.VISIBLE);
        add(scrollPanel, view, 0, child.getAlignment(), child.getFlex(), child, vertical);
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