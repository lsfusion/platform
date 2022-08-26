package lsfusion.gwt.client.form.design.view.flex;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.SizedFlexPanel;

import java.util.function.Consumer;

public class FlexTabbedPanel extends SizedFlexPanel implements IndexedPanel, RequiresResize, ProvidesResize {

    protected TabBar tabBar;

    public FlexTabbedPanel() {
        this(null, null);
    }
    public FlexTabbedPanel(String tabBarClass, Widget extraTabWidget) {
        this(tabBarClass, extraTabWidget, true);
    }
    public FlexTabbedPanel(boolean vertical) {
        this(null, null, vertical);
    }
    private FlexTabbedPanel(String tabBarClass, Widget extraTabWidget, boolean vertical) {
        super(vertical);

        FlexTabBar tabBar = new FlexTabBar(extraTabWidget, !vertical);
        if (tabBarClass != null)
            tabBar.addStyleName(tabBarClass);
        add(tabBar, GFlexAlignment.STRETCH);
        tabBar.setBeforeSelectionHandler(this::onBeforeTabSelected);
        tabBar.setSelectionHandler(FlexTabbedPanel.this::onTabSelected);
        this.tabBar = tabBar;

        addStyleName("tab-panel");
    }

    private void onBeforeTabSelected(Integer index) {
        if(beforeSelectionHandler != null)
            beforeSelectionHandler.accept(index);
    }

    private void onTabSelected(Integer tabIndex) {

        showTab(tabIndex);

        if(selectionHandler != null)
            selectionHandler.accept(tabIndex);

        scheduleOnResize(asWidget());
    }

    public static void scheduleOnResize(final Widget widget) {
        if (widget instanceof RequiresResize) {
            Scheduler.get().scheduleDeferred(() -> ((RequiresResize) widget).onResize());
        }
    }

    private Consumer<Integer> beforeSelectionHandler;
    public void setBeforeSelectionHandler(Consumer<Integer> handler) {
        assert beforeSelectionHandler == null;
        beforeSelectionHandler = handler;
    }

    private Consumer<Integer> selectionHandler;
    public void setSelectionHandler(Consumer<Integer> handler) {
        assert selectionHandler == null;
        selectionHandler = handler;
    }

    @Override
    public void onResize() {
        if (isVisible() && visibleWidget instanceof RequiresResize) {
            ((RequiresResize) visibleWidget).onResize();
        }
    }

    /**
     * Adds a widget to the tab panel. If the Widget is already attached to the
     * TabPanel, it will be moved to the right-most index.
     * @param w       the widget to be added
     * @param tabText the text to be shown on its tab
     */

    public interface AddToDeck {
        void add(SizedFlexPanel deck, int beforeIndex);
    }

    private Label createTab(String tabText, boolean wordWrap) {
        Label label = new Label(tabText, false);
        label.setStyleName("nav-link");
        return label;
    }

    // used only in error dialog
    public void addTab(Widget w, String tabText) {
        addTab(w, null, tabText);
    }

    public void addTab(Widget w, Integer index, String tabText) {
        addTab(w, index, createTab(tabText, false));
    }

    public void addTab(Widget w, Integer index, Widget tabWidget) {
        w.addStyleName("tab-pane");
        insertTab(tabWidget, index != null ? index : getTabCount(), (widgets, beforeIndex) -> widgets.addFillShrink(w, beforeIndex));
    }

    public void insertTab(String tabText, int beforeIndex, AddToDeck addToDeck) {
        insertTab(createTab(tabText, false), beforeIndex, addToDeck);
    }

    public void insertTab(Widget tabWidget, int beforeIndex, AddToDeck addToDeck) {
        tabBar.insertTab(tabWidget, beforeIndex);

        int tabIndex = getTabIndex(beforeIndex);
        addToDeck.add(this, tabIndex);
        getWidget(tabIndex).setVisible(false);
    }

    public void removeTab(int index) {
        if (index != -1) {
            if(index == getSelectedTab() && beforeSelectionHandler != null)
                beforeSelectionHandler.accept(-1);
            tabBar.removeTab(index);

            int tabIndex = getTabIndex(index);
            Widget tabWidget = getWidget(tabIndex);
            if (visibleWidget == tabWidget) {
                visibleWidget = null;
            }
            removeSized(tabWidget);
        }
    }

    private int getTabIndex(int index) {
        return index + 1; // tab bar is first
    }

    public int getTabCount() {
        return getWidgetCount() - 1; // tab bar is first
    }

    public int getSelectedTab() {
        return tabBar.getSelectedTab();
    }

    public void selectTab(int index) {
        tabBar.selectTab(index);
    }

    public void setTabCaption(int index, String caption) {
        tabBar.setTabText(index, caption);
    }

    @SuppressWarnings("UnusedDeclaration")
    public int getTabBarHeight() {
        return tabBar.asWidget().getOffsetHeight() + 5; //little extra for borders, etc.
    }

    private Widget visibleWidget;

    public void showTab(int index) {
        Widget oldWidget = visibleWidget;

        Widget newWidget;
        if(index >= 0) {
            index = getTabIndex(index); // adding flex tab bar
            checkIndexBoundsForAccess(index);

            newWidget = getWidget(index);
        } else
            newWidget = null;

        visibleWidget = newWidget;

        if (visibleWidget != oldWidget) {
            if(visibleWidget != null)
                visibleWidget.setVisible(true);
            if (oldWidget != null)
                oldWidget.setVisible(false);
        }
    }

    @Override
    public Border getOuterTopBorder() {
        return Border.HAS_MARGIN; //.nav-tabs-horz has top padding
    }

    @Override
    public FlexPanel.Border getOuterRestBorder() {
        return Border.NEED;
    }

    public void fixFlexBasis(boolean vertical) {
        impl.fixFlexBasis(this, vertical, false);
    }
}
