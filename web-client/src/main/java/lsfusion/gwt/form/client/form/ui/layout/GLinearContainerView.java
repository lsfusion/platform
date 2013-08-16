package lsfusion.gwt.form.client.form.ui.layout;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.form.shared.view.GComponent;
import lsfusion.gwt.form.shared.view.GContainer;

public class GLinearContainerView extends GAbstractContainerView {
    private final FlexPanel panel;

    private final Widget view;

    public GLinearContainerView(GContainer container) {
        super(container);

        assert container.isLinear();

        panel = new FlexPanel(container.isVertical(), container.getFlexJustify());
        panel.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);

        view = wrapWithCaption(panel);
    }

    @Override
    protected void addImpl(int index, GComponent child, Widget view) {
        panel.add(view, index, child.alignment, child.flex);
    }

    @Override
    protected void removeImpl(int index, GComponent child, Widget view) {
        panel.remove(view);
    }

    @Override
    public Widget getView() {
        return view;
    }
}
