package lsfusion.gwt.client.form.design.view.flex;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.view.MainFrame;

import java.util.function.Consumer;

/** based on from com.google.gwt.user.client.ui.TabBar */
public class FlexTabBar extends Composite implements TabBar {

    private final FlexPanel panel;

    public final boolean end;

    private int selectedTab = -1;

    private final int extraStartWidgets;

    public FlexTabBar(Widget extraTabWidget, boolean vertical, boolean end) {
        panel = new FlexPanel(vertical);

        this.end = end;

        // need this rules here (not for the panel) to avoid cascade css rules (when tab is in another tab)
        // we can't use for example .tab-bar-horz .nav-tabs rule, because sometimes nav-tabs can be this bar or sometimes can be its child
        String navTabs = "nav nav-tabs " +
                (vertical ? "nav-tabs-horz" : "nav-tabs-vert") + " " +
                (end ? "nav-tabs-end" : "nav-tabs-start");
        panel.addStyleName(navTabs);

        FlexPanel wrappedPanel;
        if(MainFrame.mobile || extraTabWidget != null) {
            // we have to wrap panel to set auto overflow, since nav-tabs needs overflow:visible (sets margin - 1, to override the border)
            // and we need overflow auto not to overlap extra widget (and in the mobile mode when the bar can be very long)
            wrappedPanel = new FlexPanel(vertical);
            if (MainFrame.mobile) // in mobile mode we don't want the bar to wrap (but to scroll to the right)
                wrappedPanel.addFill(panel);
            else
                wrappedPanel.addFillShrink(panel);
            wrappedPanel.addStyleName(vertical ? "nav-tabs-bar-wrap-horz" : "nav-tabs-bar-wrap-vert");
        } else // if we use wrap and don't have extra widgets we don't wrap tab bar into the panel, to let the tab bar overflow (just like caption panel), and save dom element
            wrappedPanel = panel;

        if (extraTabWidget == null) {
            initWidget(wrappedPanel);
        } else {
            FlexPanel tabBarContainer = new FlexPanel(vertical);

            tabBarContainer.addFillShrink(wrappedPanel);

            // to have border underneath
            // we can't set nav-tabs to the whole flex tab bar, because we want overflow:auto, and margin - 1 (to override the border) needs overflow:visible
            extraTabWidget.addStyleName(navTabs + " nav-extra-toolbar");
            tabBarContainer.addStretched(extraTabWidget);

            initWidget(tabBarContainer);
        }

        addStyleName("tab-bar");

        sinkEvents(Event.ONMOUSEDOWN);

//        addStyleName("nav-tabs-" + (vertical ? "vert" : "horz"));
//        panel.getElement().getStyle().setProperty("flexWrap", "wrap");

//        if(!MainFrame.useBootstrap) {
//            // first is to have an offset on the left, rest not sure what for (and if it has some width, when wrapping gives empty line)
//            Label first = new Label();
//
//            first.setWordWrap(true);
////        rest.setWordWrap(true);
//
//            first.setText("\u00A0");
////        rest.setText("\u00A0");
//
//            first.setStyleName("nav-item-first");
////        rest.setStyleName("nav-item-rest");
//
//            panel.add(first, GFlexAlignment.STRETCH);
//
//            extraStartWidgets = 1;
//        } else
            extraStartWidgets = 0;
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
        return panel.getWidgetCount() - extraStartWidgets;
    }

    public void insertTab(Widget widget, int beforeIndex) {
        checkInsertBeforeTabIndex(beforeIndex);

        Item delWidget;
        // it's tricky here. Since we want to keep DOM simple (and save extra element) we're using Composite
        // but if the Composite is removed, we can't create it once again so we use the Composite created previous time
        // the other solution is to add event handlers to the widget somehow (but the current solution is also not that bad)
        if(widget.getParent() instanceof Item)
            delWidget = (Item) widget.getParent();
        else {
            delWidget = new Item(widget);

            delWidget.addStyleName("nav-item");

            delWidget.addStyleName("nav-link");
            delWidget.addStyleName("link-secondary");
        }

        if(beforeIndex <= selectedTab)
            selectedTab++;

        panel.add(delWidget, beforeIndex + extraStartWidgets, GFlexAlignment.STRETCH);
        delWidget.getElement().scrollIntoView();
    }

    public void removeTab(int index) {
        checkTabIndex(index);

        if (index == selectedTab)
            selectedTab = -1;
        else if(index < selectedTab)
            selectedTab--;

        panel.remove(getTabItem(index));
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

    private Item getTabItem(int index) {
        assert (index >= 0) && (index < getTabCount()) : "Tab index out of bounds";

        return (Item) panel.getWidget(index + extraStartWidgets);
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
        int numTabs = getTabCount();

        for (int i = 0; i < numTabs; i++) {
            if (getTabItem(i) == tabWidget) {
                selectTab(i);
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
        public Item(Widget widget) {
            initWidget(widget);

            sinkEvents(Event.ONMOUSEDOWN | Event.ONKEYDOWN);
        }

        @Override
        public void onBrowserEvent(Event event) {
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
