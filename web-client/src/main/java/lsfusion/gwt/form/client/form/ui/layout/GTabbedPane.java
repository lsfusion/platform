package lsfusion.gwt.form.client.form.ui.layout;

import com.google.gwt.event.logical.shared.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.base.client.ui.GFlexAlignment;

import java.util.Iterator;

/** Based on com.google.gwt.user.client.ui.TabPanel */
public class GTabbedPane extends Composite implements HasWidgets, IndexedPanel, HasBeforeSelectionHandlers<Integer>, HasSelectionHandlers<Integer>, RequiresResize, ProvidesResize {

    private final GTabBar tabBar = new GTabBar();
    private final TabbedDeckPanel deck = new TabbedDeckPanel();

    public GTabbedPane() {
        FlexPanel panel = new FlexPanel(true);
        panel.add(tabBar, GFlexAlignment.STRETCH);
        panel.add(deck, GFlexAlignment.STRETCH, 1, "auto");

        tabBar.addBeforeSelectionHandler(new BeforeSelectionHandler<Integer>() {
            @Override
            public void onBeforeSelection(BeforeSelectionEvent<Integer> event) {
                onBeforeTabSelected(event);
            }
        });
        tabBar.addSelectionHandler(new SelectionHandler<Integer>() {
            @Override
            public void onSelection(SelectionEvent<Integer> event) {
                onTabSelected(event);
            }
        });

        initWidget(panel);

        setStyleName("gwt-TabPanel");
        deck.setStyleName("gwt-TabPanelBottom");
    }

    @Override
    public void setVisible(boolean visible) {
        getWidget().setVisible(visible);
    }

    private void onBeforeTabSelected(BeforeSelectionEvent<Integer> event) {
        BeforeSelectionEvent<Integer> panelEvent = BeforeSelectionEvent.fire(GTabbedPane.this, event.getItem());
        if (panelEvent != null && panelEvent.isCanceled()) {
            event.cancel();
        }
    }

    private void onTabSelected(SelectionEvent<Integer> event) {
        int tabIndex = event.getSelectedItem();
        deck.showWidget(tabIndex);
        SelectionEvent.fire(this, tabIndex);
    }

    @Override
    public HandlerRegistration addBeforeSelectionHandler(BeforeSelectionHandler<Integer> handler) {
        return addHandler(handler, BeforeSelectionEvent.getType());
    }

    @Override
    public HandlerRegistration addSelectionHandler(SelectionHandler<Integer> handler) {
        return addHandler(handler, SelectionEvent.getType());
    }

    @Override
    public void onResize() {
        for (Widget child : this) {
            if (child instanceof RequiresResize) {
                ((RequiresResize) child).onResize();
            }
        }
    }

    @Override
    public void add(Widget w) {
        throw new UnsupportedOperationException("A tabText parameter must be specified with add().");
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
        // Delegate updates to the TabBar to our DeckPanel implementation
        deck.insertProtected(widget, tabWidget, beforeIndex);
    }

    @Override
    public void clear() {
        while (getWidgetCount() > 0) {
            remove(getWidget(0));
        }
    }

    public int getSelectedTab() {
        return tabBar.getSelectedTab();
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

    @Override
    public Iterator<Widget> iterator() {
        // The Iterator returned by DeckPanel supports removal and will invoke
        // TabbedDeckPanel.remove(), which is an active function.
        return deck.iterator();
    }

    @Override
    public boolean remove(int index) {
        // Delegate updates to the TabBar to our DeckPanel implementation
        return deck.remove(index);
    }

    @Override
    public boolean remove(Widget widget) {
        // Delegate updates to the TabBar to our DeckPanel implementation
        return deck.remove(widget);
    }

    /**
     * Programmatically selects the specified tab and fires events.
     * @param index the index of the tab to be selected
     */
    public void selectTab(int index) {
        selectTab(index, true);
    }

    /**
     * Programmatically selects the specified tab.
     * @param index      the index of the tab to be selected
     * @param fireEvents true to fire events, false not to
     */
    public void selectTab(int index, boolean fireEvents) {
        tabBar.selectTab(index, fireEvents);
    }

    private class TabbedDeckPanel extends FlexPanel {
        private Widget visibleWidget;

        private TabbedDeckPanel() {
            super(true);
        }

        @Override
        public boolean remove(Widget w) {
            // Removal of items from the TabBar is delegated to the DeckPanel to ensure consistency
            int idx = getWidgetIndex(w);
            if (idx != -1) {
                tabBar.removeTab(idx);
                if (visibleWidget == w) {
                    visibleWidget = null;
                }
                return super.remove(w);
            }

            return false;
        }

        protected void insertProtected(Widget w, Widget tabWidget, int beforeIndex) {
            // Check to see if the TabPanel already contains the Widget. If so,
            // remove it and see if we need to shift the position to the left.
            int idx = getWidgetIndex(w);
            if (idx != -1) {
                remove(w);
                if (idx < beforeIndex) {
                    beforeIndex--;
                }
            }
            w.setVisible(false);

            tabBar.insertTab(tabWidget, beforeIndex);
            add(w, beforeIndex, GFlexAlignment.STRETCH, 1, "auto");
        }

        public void showWidget(int index) {
            checkIndexBoundsForAccess(index);

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
}
