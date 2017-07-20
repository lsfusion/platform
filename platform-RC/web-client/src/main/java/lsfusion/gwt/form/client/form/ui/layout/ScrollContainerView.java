package lsfusion.gwt.form.client.form.ui.layout;

import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.Dimension;
import lsfusion.gwt.base.client.GwtClientUtils;
import lsfusion.gwt.form.client.form.ui.layout.table.TableCaptionPanel;
import lsfusion.gwt.form.shared.view.GComponent;
import lsfusion.gwt.form.shared.view.GContainer;

import java.util.Map;

public class ScrollContainerView extends GAbstractContainerView {

    private final ScrollPanel scrollPanel;

    public ScrollContainerView(GContainer container) {
        super(container);

        assert container.isScroll();

        scrollPanel = new ScrollPanel();

        view = scrollPanel;
    }

    @Override
    protected void addImpl(int index, GComponent child, Widget view) {
        view.setWidth("100%");
        view.setHeight("100%");
        scrollPanel.add(view);
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

        for (Widget panel : scrollPanel) {
            for(Widget child : (Panel) panel) {
                if(child instanceof TableCaptionPanel) {
                    int height = container.preferredHeight > 0 ? container.preferredHeight : GwtClientUtils.calculatePreferredSize(child).height;
                    if (height > 0) {
                        child.setHeight(height + "px");
                    }
                }
            }
        }

        if(container.preferredWidth > 0)
            scrollPanel.setWidth(container.preferredWidth + "px");
        if(container.preferredHeight > 0)
            scrollPanel.setHeight(container.preferredHeight + "px");

    }

    @Override
    public Dimension getPreferredSize(Map<GContainer, GAbstractContainerView> containerViews) {
        return getChildrenStackSize(containerViews, true);
    }
}