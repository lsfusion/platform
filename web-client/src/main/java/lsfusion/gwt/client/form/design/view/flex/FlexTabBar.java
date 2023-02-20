package lsfusion.gwt.client.form.design.view.flex;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;

import java.util.function.Consumer;

/** based on from com.google.gwt.user.client.ui.TabBar */
public class FlexTabBar extends Composite implements TabBar {

    private final FlexPanel panel;

    private int selectedTab = -1;

    public FlexTabBar(Widget extraTabWidget, boolean vertical) {
        panel = new FlexPanel(vertical);
        
        panel.getElement().addClassName("tabBarItemsWrapper");
        
        if (extraTabWidget == null) {
            initWidget(panel);
        } else {
            FlexPanel tabBarContainer = new FlexPanel(vertical);
            tabBarContainer.addFillShrink(panel);
            tabBarContainer.add(extraTabWidget, GFlexAlignment.CENTER);
            initWidget(tabBarContainer);
        }

        sinkEvents(Event.ONMOUSEDOWN);

        addStyleName("nav");
        addStyleName("nav-tabs");

//        addStyleName("nav-tabs-" + (vertical ? "vert" : "horz"));
        panel.getElement().getStyle().setProperty("flexWrap", "wrap");

        // first is to have an offset on the left, rest not sure what for
        Label first = new Label();
        Label rest = new Label();

        first.setWordWrap(true);
        rest.setWordWrap(true);

        first.setText("\u00A0");
        rest.setText("\u00A0");

        first.setStyleName("nav-item-first");
        rest.setStyleName("nav-item-rest");

        panel.add(first, GFlexAlignment.STRETCH);
        panel.addFill(rest);
    }

    private Consumer<Integer> beforeSelectionHandler;
    public void setBeforeSelectionHandler(Consumer<Integer> handler) {
        this.beforeSelectionHandler = handler;
    }

    private Consumer<Integer> selectionHandler;
    public void setSelectionHandler(Consumer<Integer> handler) {
        this.selectionHandler = handler;
    }

    public int getSelectedTab() {
        return selectedTab;
    }

    public int getTabCount() {
        return panel.getWidgetCount() - 2;
    }

    public void insertTab(Widget widget, Widget extraTabWidget, int beforeIndex) {
        checkInsertBeforeTabIndex(beforeIndex);

        Item delWidget = new Item(widget, extraTabWidget);
//        delWidget.setHeight(StyleDefaults.VALUE_HEIGHT_STRING);
//        final Style delWidgetStyle = delWidget.getElement().getStyle();
//        delWidgetStyle.setDisplay(Style.Display.FLEX);
//        delWidgetStyle.setProperty("alignItems", "center");

        if(beforeIndex <= selectedTab)
            selectedTab++;

        panel.add(delWidget, beforeIndex + 1, GFlexAlignment.STRETCH);
        delWidget.getElement().scrollIntoView();

//        setStyleName(DOM.getParent(delWidget.getElement()), STYLENAME_DEFAULT + "-wrapper", true);
    }

    public void removeTab(int index) {
        checkTabIndex(index);

        if (index == selectedTab)
            selectedTab = -1;
        else if(index < selectedTab)
            selectedTab--;
        panel.remove(index + 1);
    }

    /**
     * Programmatically selects the specified tab and fires events. Use index -1
     * to specify that no tab should be selected.
     * @param index the index of the tab to be selected
     * @return <code>true</code> if successful, <code>false</code> if the change
     *         is denied by the {@link BeforeSelectionHandler}.
     */
    public void selectTab(int index) {
        if(index == selectedTab)
            return;

        checkTabIndex(index);

        beforeSelectionHandler.accept(index);

        // Check for -1.
        updateSelectionStyle(false);
        selectedTab = index;
        updateSelectionStyle(true);

        selectionHandler.accept(index);
    }

    /**
     * Enable or disable a tab. When disabled, users cannot select the tab.
     * @param index   the index of the tab to enable or disable
     * @param enabled true to enable, false to disable
     */
    public void setTabEnabled(int index, boolean enabled) {
        Item delPanel = getTabItem(index);
        delPanel.setEnabled(enabled);
        setStyleName(delPanel.getElement(), "nav-item-disabled", !enabled);
    }

    public Widget getTabWidget(int index) {
        return getTabItem(index).getWidget();
    }

    private Item getTabItem(int index) {
        assert (index >= 0) && (index < getTabCount()) : "Tab index out of bounds";

        Item item = (Item) panel.getWidget(index + 1);
        return item;
    }

    private void checkInsertBeforeTabIndex(int beforeIndex) {
        if ((beforeIndex < 0) || (beforeIndex > getTabCount())) {
            throw new IndexOutOfBoundsException();
        }
    }

    private void checkTabIndex(int index) {
        if ((index < -1) || (index >= getTabCount())) {
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * Selects the tab corresponding to the widget for the tab. To be clear the
     * widget for the tab is not the widget INSIDE of the tab; it is the widget
     * used to represent the tab itself.
     * @param tabWidget The widget for the tab to be selected
     * @return true if the tab corresponding to the widget for the tab could
     *         located and selected, false otherwise
     */
    private void selectTabByTabWidget(Widget tabWidget) {
        int numTabs = panel.getWidgetCount() - 1;

        for (int i = 1; i < numTabs; ++i) {
            if (panel.getWidget(i) == tabWidget) {
                selectTab(i - 1);
            }
        }
    }

    private void updateSelectionStyle(boolean selected) {
        int index = selectedTab;
        if(index >= 0) {
            Widget widget = getTabItem(index);
            if (selected) {
                widget.removeStyleName("link-secondary");
                widget.addStyleName("active");
            } else {
                widget.addStyleName("link-secondary");
                widget.removeStyleName("active");
            }
        }
    }

    private class Item extends Composite {
        private Widget widget;
        private boolean enabled = true;

        public Item(Widget widget, Widget extraWidget) {
            this.widget = widget;

            if(extraWidget != null) {
                FlexPanel panel = new FlexPanel();
                panel.addFillShrink(widget);
                panel.add(extraWidget, GFlexAlignment.CENTER);

                initWidget(panel);
            } else
                initWidget(widget);

            addStyleName("nav-item");

            addStyleName("nav-link");
            addStyleName("link-secondary");

            sinkEvents(Event.ONMOUSEDOWN | Event.ONKEYDOWN);
        }

        public Widget getWidget() {
            return widget;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public void onBrowserEvent(Event event) {
            if (!enabled) {
                return;
            }

            // No need for call to super.
            switch (DOM.eventGetType(event)) {
                case Event.ONMOUSEDOWN:
                    FlexTabBar.this.selectTabByTabWidget(this);
                    break;

                case Event.ONKEYDOWN:
                    if (((char) DOM.eventGetKeyCode(event)) == KeyCodes.KEY_ENTER) {
                        FlexTabBar.this.selectTabByTabWidget(this);
                    }
                    break;
            }
            super.onBrowserEvent(event);
        }
    }
}
