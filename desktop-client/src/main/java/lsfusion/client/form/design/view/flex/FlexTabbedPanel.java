package lsfusion.client.form.design.view.flex;

import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.form.design.view.FlexPanel;
import lsfusion.client.form.design.view.widget.LabelWidget;
import lsfusion.client.form.design.view.widget.Widget;
import lsfusion.interop.base.view.FlexAlignment;

import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.util.function.Consumer;

public class FlexTabbedPanel extends FlexPanel {

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
        //if (tabBarClass != null)
        //    tabBar.addStyleName(tabBarClass);
        add((Widget) tabBar, FlexAlignment.STRETCH);
        tabBar.setBeforeSelectionHandler(this::onBeforeTabSelected);
        tabBar.setSelectionHandler(FlexTabbedPanel.this::onTabSelected);
        this.tabBar = tabBar;

        setBorder(new EmptyBorder(0, 1, 1, 1)); //.gwt-TabPanel { margin: 0 1px 1px; }
    }

    private void onBeforeTabSelected(Integer index) {
        if(beforeSelectionHandler != null)
            beforeSelectionHandler.accept(index);
    }

    private void onTabSelected(Integer tabIndex) {

        showTab(tabIndex);

        if(selectionHandler != null)
            selectionHandler.accept(tabIndex);

        scheduleOnResize(this);
    }

    public static void scheduleOnResize(final Widget widget) {
        //if (widget instanceof RequiresResize) {
        //    Scheduler.get().scheduleDeferred(() -> ((RequiresResize) widget).onResize());
        //}
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

//    @Override
//    public void onResize() {
//        if (isVisible() && visibleWidget instanceof RequiresResize) {
//            ((RequiresResize) visibleWidget).onResize();
//        }
//    }

    /**
     * Adds a widget to the tab panel. If the Widget is already attached to the
     * TabPanel, it will be moved to the right-most index.
     */

    public interface AddToDeck {
        void add(FlexPanel deck, Widget widget, int beforeIndex);
    }
    public void addTab(Widget w, String tabText) {
        addTab(w, new LabelWidget(tabText));
    }

    public void addTab(Widget w, Widget tabWidget) {
        insertTab(w, tabWidget, getTabCount(), FlexPanel::addFill);
    }

    public void insertTab(Widget widget, String tabText, int beforeIndex, AddToDeck addToDeck) {
        insertTab(widget, new LabelWidget(tabText), beforeIndex, addToDeck);
    }

    public void insertTab(Widget widget, Widget tabWidget, int beforeIndex, AddToDeck addToDeck) {
        assert getWidgetIndex(widget) == -1;
        tabBar.insertTab(tabWidget, beforeIndex);

        widget.getComponent().setBorder(new LineBorder(SwingDefaults.getPanelBorderColor()));
        addToDeck.add(this, widget, getTabIndex(beforeIndex));
        widget.setVisible(false);
    }

    public boolean removeTab(int index) {
        if (index != -1) {
            if(index == getSelectedTab() && beforeSelectionHandler != null)
                beforeSelectionHandler.accept(-1);
            tabBar.removeTab(index);

            int tabIndex = getTabIndex(index);
            Widget tabWidget = getWidget(tabIndex);
            if (visibleWidget == tabWidget) {
                visibleWidget = null;
            }
            return remove(tabWidget);
        }

        return false;
    }

    private int getTabIndex(int index) {
        return index + 1; // tab bar is first
    }

    public int getTabCount() {
        return getComponentCount() - 1; // tab bar is first
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
        return SwingDefaults.getComponentHeight()  + 5; //little extra for borders, etc., getComponentHeight() - tab height
    }

    private Widget visibleWidget;

    public void showTab(int index) {
        index = getTabIndex(index); // adding flex tab bar
        //checkIndexBoundsForAccess(index);

        Widget oldWidget = visibleWidget;
        visibleWidget = getWidget(index);

        if (visibleWidget != oldWidget) {
            visibleWidget.setVisible(true);
            if (oldWidget != null) {
                oldWidget.setVisible(false);
            }
        }
    }
}
