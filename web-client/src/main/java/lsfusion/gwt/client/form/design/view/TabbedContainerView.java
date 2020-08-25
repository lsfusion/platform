package lsfusion.gwt.client.form.design.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.flex.FlexTabbedPanel;

import java.util.ArrayList;
import java.util.Map;

import static lsfusion.gwt.client.base.GwtSharedUtils.relativePosition;

public class TabbedContainerView extends GAbstractContainerView {

    protected final Panel panel;

    private final Widget view;

    protected final ArrayList<GComponent> visibleChildren = new ArrayList<>();

    protected GComponent currentChild;

    public static class Panel extends FlexTabbedPanel {

        public void insertTab(GComponent child, Widget childView, String tabTitle, int index) {
            child.installMargins(childView);

            // not sure why but this wrapping is necessary (otherwise widgets are not hidden)
            // + in that case we'll need to "override" insert to deck method to fill correct base sizes
            FlexPanel proxyPanel = new FlexPanel(true);
            GAbstractContainerView.add(proxyPanel, childView, child, 0);

            insert(proxyPanel, tabTitle, index);
        }
    }

    public TabbedContainerView(final GFormController formController, final GContainer container) {
        super(container);

        assert container.isTabbed();

        panel = new Panel();

        if (container.children.size() > 0) {
            currentChild = container.children.get(0);
        }

        panel.setSelectionHandler(index -> {
            onTabSelected(index, formController, container);
        });

        view = initBorder(panel);
    }

    private int getTabIndex(GComponent component) {
        for(int i=0,size=visibleChildren.size();i<size;i++)
            if(visibleChildren.get(i).equals(component))
                return i;
        return -1;
    }

    public void activateTab(GComponent component) {
        int index = getTabIndex(component);
        if(index >= 0) {
            currentChild = component;
            panel.selectTab(index);
        }
    }

    public void updateTabCaption(GContainer container) {
        int index = getTabIndex(container);
        if(index >= 0)
            panel.setTabCaption(index, container.caption);
    }

    protected void onTabSelected(int selectedIndex, GFormController formController, GContainer container) {
        if (selectedIndex >= 0) {
            GComponent selectedChild = visibleChildren.get(selectedIndex);
            if (currentChild != selectedChild) {
                currentChild = selectedChild;
                formController.setTabVisible(container, selectedChild);
            }
        }
    }

    @Override
    protected void addImpl(int index, GComponent child, Widget view) {
        //adding is done in updateLayout()
    }

    @Override
    protected void removeImpl(int index, GComponent child, Widget view) {
        int visibleIndex = visibleChildren.indexOf(child);
        if (visibleIndex != -1) {
            panel.remove(visibleIndex);
            visibleChildren.remove(visibleIndex);
        }
    }

    @Override
    public Widget getView() {
        return view;
    }

    @Override
    public void updateLayout() {
        super.updateLayout();
        int childCnt = childrenViews.size();
        for (int i = 0; i < childCnt; i++) {
            GComponent child = children.get(i);
            Widget childView = childrenViews.get(i);

            int index = visibleChildren.indexOf(child);
            if (childView.isVisible()) {
                if (index == -1) {
                    index = relativePosition(child, children, visibleChildren);
                    visibleChildren.add(index, child);
                    panel.insertTab(child, childView, getTabTitle(child), index);
                    // updateTabCaption(child);
                }
            } else if (index != -1) {
                visibleChildren.remove(index);
                panel.remove(index);
            }
        }
        ensureTabSelection();
    }

    @Override
    public Dimension getMaxPreferredSize(Map<GContainer, GAbstractContainerView> containerViews) {
        int selected = panel.getSelectedTab();
        if (selected != -1) {
            Dimension dimensions = getChildMaxPreferredSize(containerViews, selected);
            dimensions.height += panel.getTabBarHeight() + 5; //little extra for borders, etc.
            return dimensions;
        }
        return new Dimension(0, 0);
    }

    private void ensureTabSelection() {
        if (panel.getSelectedTab() == -1 && panel.getWidgetCount() != 0) {
            panel.selectTab(0);
        }
    }

    protected static String getTabTitle(GComponent child) {
        String tabCaption = null;
        if (child instanceof GContainer) {
            tabCaption = ((GContainer) child).caption;
        }
        if (tabCaption == null) {
            tabCaption = "";
        }
        return tabCaption;
    }

    public boolean isTabVisible(GComponent component) {
        return visibleChildren.contains(component);
    }
}
