package lsfusion.gwt.form.client.form.ui.layout;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.form.client.form.ui.GFormController;
import lsfusion.gwt.form.shared.view.GComponent;
import lsfusion.gwt.form.shared.view.GContainer;

import java.util.ArrayList;

import static lsfusion.gwt.base.shared.GwtSharedUtils.relativePosition;

public class GTabbedContainerView extends GAbstractContainerView {

    private final GTabbedPane tabsPanel;
    private final Widget view;

    private final ArrayList<GComponent> visibleChildren = new ArrayList<GComponent>();

    private GComponent currentChild;

    public GTabbedContainerView(final GFormController formController, final GContainer container) {
        super(container);

        tabsPanel = new GTabbedPane();
        view = tabsPanel;

        if (container.children.size() > 0) {
            currentChild = container.children.get(0);
        }

        tabsPanel.addSelectionHandler(new SelectionHandler<Integer>() {
            @Override
            public void onSelection(SelectionEvent<Integer> e) {
                int index = e.getSelectedItem();
                if (index >= 0) {
                    GComponent selectedChild = visibleChildren.get(index);
                    if (currentChild != selectedChild) {
                        currentChild = selectedChild;
                        formController.setTabVisible(container, selectedChild);
                    }
                }
            }
        });
    }

    @Override
    protected void addImpl(int index, GComponent child, Widget view) {
        //adding is done in updateLayout()
    }

    @Override
    protected void removeImpl(int index, GComponent child, Widget view) {
        int visibleIndex = visibleChildren.indexOf(child);
        if (visibleIndex != -1) {
            tabsPanel.remove(visibleIndex);
            visibleChildren.remove(visibleIndex);
        }
    }

    @Override
    public Widget getView() {
        return view;
    }

    @Override
    void updateLayout() {
        int childCnt = childrenViews.size();
        for (int i = 0; i < childCnt; i++) {
            GComponent child = children.get(i);
            Widget childView = childrenViews.get(i);

            int index = visibleChildren.indexOf(child);
            if (childView.isVisible()) {
                if (index == -1) {
                    index = relativePosition(child, children, visibleChildren);
                    visibleChildren.add(index, child);

                    FlexPanel proxyPanel = new FlexPanel(true);
                    proxyPanel.addFill(childView);

                    tabsPanel.insert(proxyPanel, getTabTitle(child), index);
                }
            } else if (index != -1) {
                visibleChildren.remove(index);
                tabsPanel.remove(index);
            }
        }
        ensureTabSelection();
    }

    private void ensureTabSelection() {
        if (tabsPanel.getSelectedTab() == -1 && tabsPanel.getWidgetCount() != 0) {
            tabsPanel.selectTab(0);
        }
    }

    private String getTabTitle(GComponent child) {
        String tabCaption = null;
        if (child instanceof GContainer) {
            tabCaption = ((GContainer) child).caption;
        }
        if (tabCaption == null) {
            tabCaption = "";
        }
        return tabCaption;
    }
}
