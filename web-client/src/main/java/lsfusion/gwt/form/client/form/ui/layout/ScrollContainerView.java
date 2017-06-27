package lsfusion.gwt.form.client.form.ui.layout;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.Dimension;
import lsfusion.gwt.base.client.GwtClientUtils;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.base.client.ui.GFlexAlignment;
import lsfusion.gwt.base.client.ui.HasPreferredSize;
import lsfusion.gwt.form.client.form.ui.layout.table.TableCaptionPanel;
import lsfusion.gwt.form.shared.view.GComponent;
import lsfusion.gwt.form.shared.view.GContainer;

import java.util.Map;

public class ScrollContainerView extends GAbstractContainerView {

    private final FlexPanel scrollPanel;
    private final boolean vertical = true;

    public ScrollContainerView(GContainer container) {
        super(container);

        assert container.isScroll();

        scrollPanel = new FlexPanel(vertical);
        Style style = scrollPanel.getElement().getStyle();
        style.setOverflowY(Style.Overflow.AUTO); // scroll

        view = scrollPanel;
    }

    @Override
    protected void addImpl(int index, GComponent child, Widget view) {
        assert child.flex == 1 && child.alignment == GFlexAlignment.STRETCH; // временные assert'ы чтобы проверить обратную совместимость
        add(scrollPanel, view, 0, child.alignment, child.flex, child, vertical);
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

//        for (Widget panel : scrollPanel) {
//            for(Widget child : (Panel) panel) {
//                if(child instanceof TableCaptionPanel) {
//                    int height = container.preferredHeight > 0 ? container.preferredHeight : GwtClientUtils.calculatePreferredSize(child).height;
//                    if (height > 0) {
//                        child.setHeight(height + "px");
//                    }
//                }
//            }
//        }
//
//        if(container.preferredWidth > 0)
//            scrollPanel.setWidth(container.preferredWidth + "px");
//        if(container.preferredHeight > 0)
//            scrollPanel.setHeight(container.preferredHeight + "px");
//
    }

    @Override
    public Dimension getPreferredSize(Map<GContainer, GAbstractContainerView> containerViews) {
        return getChildrenStackSize(containerViews, true);
    }
}