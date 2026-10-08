package lsfusion.client.form.design.view;

import lsfusion.base.BaseUtils;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.client.form.design.view.flex.FlexTabbedPanel;
import lsfusion.client.form.design.view.widget.Widget;

import java.util.ArrayList;

import static lsfusion.base.BaseUtils.relativePosition;

public class TabbedClientContainerView extends AbstractClientContainerView {

    protected final FlexTabbedPanel panel;

    protected final ArrayList<ClientComponent> visibleChildren = new ArrayList<>();

    // the tab the server has active, as far as the client knows: selecting it sends nothing
    protected ClientComponent currentChild;

    public TabbedClientContainerView(final ClientFormController formController, final ClientContainer container) {
        super(container);

        assert container.tabbed;

        panel = new FlexTabbedPanel(vertical);

        panel.setSelectionHandler(index -> {
            onTabSelected(index, formController, container);
        });
    }

    @Override
    protected FlexPanel wrapBorderImpl(ClientComponent child) {
        return null;
    }

    private int getTabIndex(ClientComponent component) {
        for(int i=0,size=visibleChildren.size();i<size;i++)
            if(BaseUtils.hashEquals(visibleChildren.get(i), component))
                return i;
        return -1;
    }

    public void activateTab(ClientComponent component) {
        int index = getTabIndex(component);
        if(index >= 0) {
            panel.selectTab(index);
        }
    }

    // a tab the server has made active (ACTIVATE TAB): selected with nothing sent, now if it is shown, else when the
    // panel selects a tab (ensureTabSelection) or once it is shown (activateTab)
    public void setActiveTab(ClientComponent component) {
        currentChild = component;
        activateTab(component);
    }

    // a tab the server has made active, selected after the layout once it is shown: only while it is the server's tab -
    // a later one of the container, reported or the user's, takes its place, and selecting this one would send it
    public void activateServerTab(ClientComponent component) {
        if (component.equals(currentChild))
            activateTab(component);
    }

    public void updateCaption(ClientContainer container) {
        int index = getTabIndex(container);
        if(index >= 0)
            panel.setTabCaption(index, container.caption);
    }

    protected void onTabSelected(int selectedIndex, ClientFormController formController, ClientContainer container) {
        if (selectedIndex >= 0) {
            ClientComponent selectedChild = visibleChildren.get(selectedIndex);
            if (currentChild != selectedChild) {
                currentChild = selectedChild;
                formController.setTabActive(container, selectedChild);
            }
        }
    }

    @Override
    protected void addImpl(int index, ClientComponent child, Widget view) {
        //adding is done in updateLayout()
    }

    @Override
    protected void removeImpl(int index, ClientComponent child) {
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
    public void updateLayout(boolean[] childrenVisible) {
        for (int i = 0, size = children.size(); i < size; i++) {
            ClientComponent child = children.get(i);

            int index = visibleChildren.indexOf(child);
            if (childrenVisible[i]) {
                if (index == -1) {
                    index = relativePosition(child, children, visibleChildren);
                    visibleChildren.add(index, child);
                    panel.insertTab(childrenViews.get(i), getTabTitle(child), index, (deck, widget, beforeIndex) -> add(deck, widget, child, beforeIndex));
                }
            } else if (index != -1) {
                visibleChildren.remove(index);
                panel.removeTab(index);
            }
        }
        ensureTabSelection();

        super.updateLayout(childrenVisible);
    }

    // the tab the server has active if it is shown, else the first one, which is then sent as the client's choice
    private void ensureTabSelection() {
        if (panel.getSelectedTab() == -1 && panel.getTabCount() != 0) {
            int index = visibleChildren.indexOf(currentChild);
            panel.selectTab(index >= 0 ? index : 0);
        }
    }

    protected static String getTabTitle(ClientComponent child) {
        String tabCaption = null;
        if (child instanceof ClientContainer) {
            tabCaption = ((ClientContainer) child).caption;
        }
        if (tabCaption == null) {
            tabCaption = "";
        }
        return tabCaption;
    }

    public boolean isTabVisible(ClientComponent component) {
        return visibleChildren.contains(component);
    }
}
