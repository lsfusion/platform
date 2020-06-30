package lsfusion.gwt.client.form.design.view.flex;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.logical.shared.*;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.base.view.FixFlexBasisComposite;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;

import java.util.function.Consumer;

public class FlexTabbedPanel extends FixFlexBasisComposite implements IndexedPanel, RequiresResize, ProvidesResize {
    private final FlexPanel panel;
    protected TabBar tabBar;
    protected TabDeck deck;

    public FlexTabbedPanel() {
        FlexTabBar tabBar = new FlexTabBar();
        TabbedDeckPanel deck = new TabbedDeckPanel();

        panel = new FlexPanel(true);
        panel.add(tabBar, GFlexAlignment.STRETCH);
        panel.addFill(deck);

        this.tabBar = tabBar;
        this.deck = deck;

        tabBar.setBeforeSelectionHandler(this::onBeforeTabSelected);
        tabBar.setSelectionHandler(index -> FlexTabbedPanel.this.onTabSelected(index));

        initWidget(panel);

        setStyleName("gwt-TabPanel");
        deck.setStyleName("gwt-TabPanelBottom");
    }

    private void onBeforeTabSelected(Integer index) {
        if(beforeSelectionHandler != null)
            beforeSelectionHandler.accept(index);
    }

    private void onTabSelected(Integer tabIndex) {

        deck.showWidget(tabIndex);

        if(selectionHandler != null)
            selectionHandler.accept(tabIndex);

        scheduleOnResize(asWidget());
    }

    public static void scheduleOnResize(final Widget widget) {
        if (widget instanceof RequiresResize) {
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    ((RequiresResize) widget).onResize();
                }
            });
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
        if (isVisible()) {
            deck.onResize();
        }
    }

    /**
     * Adds a widget to the tab panel. If the Widget is already attached to the
     * TabPanel, it will be moved to the right-most index.
     * @param w       the widget to be added
     * @param tabText the text to be shown on its tab
     */
    public void add(Widget w, String tabText) {
        insert(w, tabText, getWidgetCount());
    }

    /**
     * Adds a widget to the tab panel. If the Widget is already attached to the
     * TabPanel, it will be moved to the right-most index.
     * @param w       the widget to be added
     * @param tabText the text to be shown on its tab
     * @param asHTML  <code>true</code> to treat the specified text as HTML
     */
    public void add(Widget w, String tabText, boolean asHTML) {
        insert(w, tabText, asHTML, getWidgetCount());
    }

    /**
     * Adds a widget to the tab panel. If the Widget is already attached to the
     * TabPanel, it will be moved to the right-most index.
     * @param w         the widget to be added
     * @param tabWidget the widget to be shown in the tab
     */
    public void add(Widget w, Widget tabWidget) {
        insert(w, tabWidget, getWidgetCount());
    }

    public void insert(Widget widget, String tabText, int beforeIndex) {
        insert(widget, tabText, false, beforeIndex);
    }

    public void insert(Widget widget, String tabText, boolean asHTML, int beforeIndex) {
        // Delegate updates to the TabBar to our DeckPanel implementation
        insert(widget, asHTML ? new HTML(tabText, false) : new Label(tabText, false), beforeIndex);
    }

    public void insert(Widget widget, Widget tabWidget, int beforeIndex) {
        // Check to see if the TabPanel already contains the Widget. If so,
        // remove it and see if we need to shift the position to the left.
        int ind = getWidgetIndex(widget);
        if (ind != -1) {
            remove(ind);
            if (ind < beforeIndex) {
                beforeIndex--;
            }
        }
        tabBar.insertTab(tabWidget, beforeIndex);
        deck.insert(widget, beforeIndex);
    }

    @Override
    public boolean remove(int index) {
        if (index != -1) {
            if(index == getSelectedTab() && beforeSelectionHandler != null)
                beforeSelectionHandler.accept(-1);
            tabBar.removeTab(index);
            return deck.remove(index);
        }

        return false;
    }

    @Override
    public Widget getWidget(int index) {
        return deck.getWidget(index);
    }

    @Override
    public int getWidgetCount() {
        return deck.getWidgetCount();
    }

    @Override
    public int getWidgetIndex(Widget widget) {
        return deck.getWidgetIndex(widget);
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
}
