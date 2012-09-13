package platform.gwt.form2.client.form.ui.container;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.form2.client.form.ui.GFormController;
import platform.gwt.form2.shared.view.GComponent;
import platform.gwt.form2.shared.view.GContainer;

public class GFormTabbedPane extends GAbstractFormContainer {
    private TabPanel tabsPanel;

    public GFormTabbedPane(final GFormController formController, final GContainer key) {
        this.key = key;

        tabsPanel = new TabPanel();
        tabsPanel.getDeckPanel().setHeight("100%");

        tabsPanel.addSelectionHandler(new SelectionHandler<Integer>() {
            @Override
            public void onSelection(SelectionEvent<Integer> e) {
                int index = e.getSelectedItem();
                formController.setTabVisible(key, (GComponent) childrenViews.keySet().toArray()[index]);
            }
        });
    }

    @Override
    public Widget getUndecoratedView() {
        return tabsPanel;
    }

    @Override
    protected void addToContainer(GComponent childKey, Widget childView, int position) {
        String tabTitle = null;
        if (childKey instanceof GContainer) {
            tabTitle = ((GContainer) childKey).title;
        }
        if (tabTitle == null) {
            tabTitle = "";
        }

        tabsPanel.insert(childView, tabTitle, position == -1 ? tabsPanel.getWidgetCount() : position);

        if (tabsPanel.getTabBar().getSelectedTab() == -1) {
            tabsPanel.selectTab(0);
        }
    }

    @Override
    protected void removeFromContainer(GComponent childKey, Widget childView) {
        tabsPanel.remove(childView);
    }

    @Override
    protected boolean containerHasChild(Widget childView) {
        return tabsPanel.getWidgetIndex(childView) != -1;
    }

    @Override
    public void setChildSize(GComponent child, String width, String height) {
        Widget childView = childrenViews.get(child);
        if (childView != null) {
            if (width != null) {
                childView.setWidth(width);
            }
            if (height != null) {
                childView.setHeight(height);
            }
        }
    }
}
