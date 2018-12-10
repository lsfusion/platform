package lsfusion.gwt.client.form.form.ui.layout;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.form.form.ui.GFormController;
import lsfusion.gwt.shared.form.view.GComponent;
import lsfusion.gwt.shared.form.view.GContainer;

import java.util.ArrayList;
import java.util.Map;

import static lsfusion.gwt.client.base.GwtClientUtils.*;
import static lsfusion.gwt.shared.base.GwtSharedUtils.relativePosition;

public class TabbedContainerView extends GAbstractContainerView {

    protected interface TabbedDelegate {
        HandlerRegistration addSelectionHandler(SelectionHandler<Integer> selectionHandler);

        boolean remove(int index);

        void insertTab(GComponent child, Widget childView, String tabTitle, int index);

        int getSelectedTab();

        int getWidgetCount();

        void selectTab(int i);

        Widget asWidget();

        int getTabBarHeight();
    }

    protected final TabbedDelegate tabbedDelegate;

    protected final ArrayList<GComponent> visibleChildren = new ArrayList<>();

    protected GComponent currentChild;

    public TabbedContainerView(final GFormController formController, final GContainer container, final TabbedDelegate delegate) {
        super(container);

        assert container.isTabbed();

        tabbedDelegate = delegate;

        if (container.children.size() > 0) {
            currentChild = container.children.get(0);
        }

        tabbedDelegate.addSelectionHandler(new SelectionHandler<Integer>() {
            @Override
            public void onSelection(SelectionEvent<Integer> e) {
                onTabSelected(e.getSelectedItem(), formController, container);
                scheduleOnResize(tabbedDelegate.asWidget());
            }
        });
    }

    public void activateTab(int i) {
        currentChild = container.children.get(i);  // изменение сервер уже в курсе об изменениях, поэтому пометим так
        tabbedDelegate.selectTab(i);
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
            tabbedDelegate.remove(visibleIndex);
            visibleChildren.remove(visibleIndex);
        }
    }

    @Override
    public Widget getView() {
        return tabbedDelegate.asWidget();
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
                    tabbedDelegate.insertTab(child, childView, getTabTitle(child), index);
                }
            } else if (index != -1) {
                visibleChildren.remove(index);
                tabbedDelegate.remove(index);
            }
        }
        ensureTabSelection();
    }

    @Override
    public Dimension getMaxPreferredSize(Map<GContainer, GAbstractContainerView> containerViews) {
        int selected = tabbedDelegate.getSelectedTab();
        if (selected != -1) {
            Dimension dimensions = getChildMaxPreferredSize(containerViews, selected);
            dimensions.height += tabbedDelegate.getTabBarHeight() + 5; //little extra for borders, etc.
            return dimensions;
        }
        return new Dimension(0, 0);
    }

    private void ensureTabSelection() {
        if (tabbedDelegate.getSelectedTab() == -1 && tabbedDelegate.getWidgetCount() != 0) {
            tabbedDelegate.selectTab(0);
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
