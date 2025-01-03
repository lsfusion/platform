package lsfusion.gwt.client.form.design.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.GFormComponent;
import lsfusion.gwt.client.form.design.view.flex.FlexTabbedPanel;

import java.util.ArrayList;

public class TabbedContainerView extends GAbstractContainerView {

    protected final FlexTabbedPanel panel;

    protected final ArrayList<GComponent> visibleChildren = new ArrayList<>();

    protected GComponent currentChild;

    public TabbedContainerView(final GFormController formController, final GContainer container) {
        super(container);

        assert container.tabbed;

        panel = new FlexTabbedPanel(null, vertical, false);

        panel.setSelectionHandler(index -> {
            onTabSelected(index, formController, container);
        });
    }

    @Override
    protected Widget wrapBorderImpl(int index) {
//      this wrapping is necessary because:
//          we want border around the container
//          we want padding (not margin) to be "scrolled"
//          updateContainersVisibility (automatical showing / hiding containers) uses setVisible, as well as TabbedDeckPanel (switching widgets), so they conflict with each other (however in current implementation only for base components)
        FlexPanel proxyPanel = new FlexPanel(!vertical);
        GwtClientUtils.addClassName(proxyPanel, "tab-pane");
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
    protected void addImpl(int index) {
        //adding is done in updateLayout()
    }

    @Override
    protected void removeImpl(int index) {
        int visibleIndex = visibleChildren.indexOf(children.get(index));
        if (visibleIndex != -1) {
            removeTab(visibleIndex);
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
                if (index == -1)
                    insertTab(child, i);
            } else if (index != -1) {
                removeTab(index);
            }
        }
        ensureTabSelection();

        super.updateLayout(requestIndex, childrenVisible);
    }

    private void insertTab(GComponent child, int i) {
        int index = GwtSharedUtils.relativePosition(child, children, visibleChildren);
        visibleChildren.add(index, child);
        // just like in grid it's not clear how to manage inline elements, so we'll just assert that there are no such elements in the tabbed container
        panel.insertTab(childrenCaptions.get(i).widget.widget, index, beforeIndex -> addChildrenWidget(panel, i, beforeIndex));
    }

    private void removeTab(int visibleIndex) {
        visibleChildren.remove(visibleIndex);
        panel.removeTab(visibleIndex, index -> removeChildrenWidget(panel, visibleIndex, index));
    }

    private void ensureTabSelection() {
        if (panel.getSelectedTab() == -1 && panel.getTabCount() != 0) {
            panel.selectTab(0);
        }
    }

    public boolean isTabVisible(GComponent component) {
        return visibleChildren.contains(component);
    }
}
