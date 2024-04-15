package lsfusion.gwt.client.form.design.view.flex;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.SizedFlexPanel;

import java.util.function.Consumer;

public class FlexTabbedPanel extends SizedFlexPanel implements IndexedPanel, RequiresResize, ProvidesResize {

    protected TabBar tabBar;
    public final boolean tabEnd;

    public FlexTabbedPanel() {
        this(null, false);
    }
    public FlexTabbedPanel(Widget extraTabWidget, boolean end) {
        this(extraTabWidget, true, end);
    }

    public FlexTabbedPanel(Widget extraTabWidget, boolean vertical, boolean end) {
        super(vertical);

        FlexTabBar tabBar = new FlexTabBar(extraTabWidget, !vertical, end);
        add(tabBar, GFlexAlignment.STRETCH);
        tabBar.setBeforeSelectionHandler(this::onBeforeTabSelected);
        tabBar.setSelectionHandler(FlexTabbedPanel.this::onTabSelected);
        this.tabBar = tabBar;

        addStyleName("tab-panel");
        addStyleName(end ? "tab-panel-end" : "tab-panel-start");
        tabEnd = end;
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
        void add(int beforeIndex);
    }
    public interface RemoveFromDeck {
        void remove(int index);
    }

    private Label createTab(String tabText, boolean wordWrap) {
        return new Label(tabText, wordWrap);
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
        insertTab(tabWidget, index != null ? index : getTabCount(), (beforeIndex) -> addFillShrink(w, beforeIndex));
    }

    public void insertTab(Widget tabWidget, int beforeIndex, AddToDeck addToDeck) {
        tabBar.insertTab(tabWidget, beforeIndex);

        int tabIndex = getTabIndex(beforeIndex);
        addToDeck.add(tabIndex);
        getWidget(tabIndex).setVisible(false);
    }

    public void removeTab(int removeIndex) {
        removeTab(removeIndex, this::removeSized);
    }

    public void removeTab(int index, RemoveFromDeck removeFromDeck) {
        if(index == getSelectedTab() && beforeSelectionHandler != null)
            beforeSelectionHandler.accept(-1);
        tabBar.removeTab(index);

        int tabIndex = getTabIndex(index);
        if (visibleWidget == getWidget(tabIndex)) {
            visibleWidget = null;
        }
        removeFromDeck.remove(tabIndex);
    }

    private int getTabIndex(int index) {
        return tabEnd ? index : index + 1; // if tab bar is first then shifting the index
    }

    public int getTabCount() {
        return getWidgetCount() - 1; // tab bar is included
    }

    public int getSelectedTab() {
        return tabBar.getSelectedTab();
    }

    public Widget getTabBar() {
        return tabBar.asWidget();
    }

    public void selectTab(int index) {
        tabBar.selectTab(index);
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

    public void fixFlexBasis(boolean vertical) {
        impl.fixFlexBasis(this, vertical, false);
    }
}
