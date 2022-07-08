package lsfusion.gwt.client.form.design.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.GFormComponent;
import lsfusion.gwt.client.form.design.view.flex.FlexTabbedPanel;

import java.util.ArrayList;

import static lsfusion.gwt.client.base.GwtSharedUtils.relativePosition;

public class TabbedContainerView extends GAbstractContainerView {

    protected final FlexTabbedPanel panel;

    protected final ArrayList<GComponent> visibleChildren = new ArrayList<>();

    protected GComponent currentChild;

    public TabbedContainerView(final GFormController formController, final GContainer container) {
        super(container);

        assert container.tabbed;

        panel = new FlexTabbedPanel(vertical);

        if (container.children.size() > 0) {
            currentChild = container.children.get(0);
        }

        panel.setSelectionHandler(index -> {
            onTabSelected(index, formController, container);
        });
    }

    @Override
    protected FlexPanel wrapBorderImpl(GComponent child) {
//      this wrapping is necessary because:
//          we want border around the container
//          updateContainersVisibility (automatical showing / hiding containers) uses setVisible, as well as TabbedDeckPanel (switching widgets), so they conflict with each other (however in current implementation only for base components)
        FlexPanel proxyPanel = new FlexPanel(!vertical);
        proxyPanel.setStyleName("gwt-TabPanelBottom");
        return proxyPanel;
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

    public void activateLastTab() {
        int index = visibleChildren.size() - 1;
        if(index >= 0) {
            currentChild = visibleChildren.get(index);
            panel.selectTab(index);
        }
    }

    public void updateCaption(GContainer container) {
        int index = getTabIndex(container);
        if(index >= 0)
            panel.setTabCaption(index, container.caption);
    }

    protected void onTabSelected(int selectedIndex, GFormController formController, GContainer container) {
        if (selectedIndex >= 0) {
            GComponent selectedChild = visibleChildren.get(selectedIndex);
            if (currentChild != selectedChild && !(selectedChild instanceof GFormComponent)) {
                currentChild = selectedChild;
                formController.setTabActive(container, selectedChild);
            }
        }
    }

    @Override
    protected void addImpl(int index, GComponent child, Widget view, ResizableComplexPanel attachContainer) {
        //adding is done in updateLayout()
    }

    @Override
    protected void removeImpl(int index, GComponent child) {
        int visibleIndex = visibleChildren.indexOf(child);
        if (visibleIndex != -1) {
            panel.removeTab(visibleIndex);
            visibleChildren.remove(visibleIndex);
        }
    }

    @Override
    public Widget getView() {
        return panel;
    }

    @Override
    public void updateLayout(long requestIndex, boolean[] childrenVisible) {
        for (int i = 0, size = children.size(); i < size; i++) {
            GComponent child = children.get(i);

            int index = visibleChildren.indexOf(child);
            if (childrenVisible[i]) {
                if (index == -1) {
                    index = relativePosition(child, children, visibleChildren);
                    visibleChildren.add(index, child);
                    final int fi = i;
                    panel.insertTab(getTabTitle(child), index, (deck, beforeIndex) -> addChildrenWidget(deck, fi, beforeIndex));
                }
            } else if (index != -1) {
                visibleChildren.remove(index);
                panel.removeTab(index);
            }
        }
        ensureTabSelection();

        super.updateLayout(requestIndex, childrenVisible);
    }

    private void ensureTabSelection() {
        if (panel.getSelectedTab() == -1 && panel.getTabCount() != 0) {
            panel.selectTab(0);
        }
    }

    protected static String getTabTitle(GComponent child) {
        String tabCaption = null;
        if (child instanceof GContainer) {
            tabCaption = ((GContainer) child).caption;
        } else if (child instanceof GFormComponent) {
            tabCaption = ((GFormComponent) child).caption;
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
