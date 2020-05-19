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

        view = initBorder(scrollPanel);
    }

    private FlexPanel proxyPanel;
    private Widget proxyView;
    @Override
    protected void addImpl(int index, GComponent child, Widget view) {
        assert child.getFlex() == 1 && child.getAlignment() == GFlexAlignment.STRETCH; // временные assert'ы чтобы проверить обратную совместимость
//        if(child.preferredHeight == 1) { // panel тем же базисом и flex'ом (assert что 1)
//            proxyPanel = new FlexPanel(vertical);
//
//            proxyPanel.add(view, child.alignment, child.flex > 0 ? 1 : 0);
//            addedPanel.set(proxyPanel);
//            addedPanel = null;
//
//            proxyView = view;
//            view = proxyPanel;
//        }
        // возможно надо попроставлять как внизу компонентам MaxPreferredSize ??
        if(1!=1) GwtClientUtils.calculateMaxPreferredSize(view); // .height
        view.getElement().getStyle().setOverflowY(Style.Overflow.VISIBLE);
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

    public void updateLayout() {
        super.updateLayout();
//        if(proxyPanel != null) {
//            if(proxyView instanceof FlexPanel) {
//                for(Widget child : ((FlexPanel)proxyView)) {
//                        int height = GwtClientUtils.calculatePreferredSize(child).height;
//                        if (height > 0) {
//                            ((FlexPanel)proxyView).setChildFlexBasis(child, height);
////                            child.setHeight(height + "px");
//                        }
//                }
//            } else
//                proxyPanel.setChildFlexBasis(proxyView, GwtClientUtils.calculatePreferredSize(proxyView).height);
//        }
    }
}