package lsfusion.gwt.client.form.design.view.flex;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.impl.FocusImpl;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;

import java.util.function.Consumer;

/** based on from com.google.gwt.user.client.ui.TabBar */
public class FlexTabBar extends Composite implements TabBar {

    public interface Tab extends HasAllKeyHandlers, HasClickHandlers {
    }

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

    public void insertTab(Widget widget, int beforeIndex) {
        checkInsertBeforeTabIndex(beforeIndex);

        ClickDelegatePanel delWidget = new ClickDelegatePanel(widget);
        delWidget.addStyleName("nav-item");
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
        assert (index >= 0) && (index < getTabCount()) : "Tab index out of bounds";

        // Style the wrapper
        ClickDelegatePanel delPanel = (ClickDelegatePanel) panel.getWidget(index + 1);
        delPanel.setEnabled(enabled);
        setStyleName(delPanel.getElement(), "nav-item-disabled", !enabled);
    }

    /**
     * Sets a tab's contents via HTML.
     * <p/>
     * Use care when setting an object's HTML; it is an easy way to expose
     * script-based security problems. Consider using
     * {@link #setTabText(int, String)} or {@link #setTabHTML(int, SafeHtml)}
     * whenever possible.
     * @param index the index of the tab whose HTML is to be set
     * @param html  the tab new HTML
     */
    public void setTabHTML(int index, String html) {
        assert (index >= 0) && (index < getTabCount()) : "Tab index out of bounds";

        ClickDelegatePanel delPanel = (ClickDelegatePanel) panel.getWidget(index + 1);
        SimplePanel focusablePanel = delPanel.getFocusablePanel();
        focusablePanel.setWidget(new HTML(html, false));
    }

    /**
     * Sets a tab's contents via safe html.
     * @param index the index of the tab whose HTML is to be set
     * @param html  the tab new HTML
     */
    public void setTabHTML(int index, SafeHtml html) {
        setTabHTML(index, html.asString());
    }

    /**
     * Sets a tab's text contents.
     * @param index the index of the tab whose text is to be set
     * @param text  the object's new text
     */
    public void setTabText(int index, String text) {
        assert (index >= 0) && (index < getTabCount()) : "Tab index out of bounds";

        ClickDelegatePanel delPanel = (ClickDelegatePanel) panel.getWidget(index + 1);
        SimplePanel focusablePanel = delPanel.getFocusablePanel();

        ((Label)focusablePanel.getWidget()).setText(text);
        // It is not safe to check if the current widget is an instanceof Label and
        // reuse it here because HTML is an instanceof Label. Leaving an HTML would
        // throw off the results of getTabHTML(int).
//        focusablePanel.setWidget(new Label(text, false));
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
            Widget widget = ((ClickDelegatePanel) panel.getWidget(index + 1)).getFocusablePanel().getWidget();
            if (selected) {
                widget.addStyleName("active");
            } else {
                widget.removeStyleName("active");
            }
        }
    }

    /**
     * <code>ClickDelegatePanel</code> decorates any widget with the minimal
     * amount of machinery to receive clicks for delegation to the parent.
     * {@link SourcesClickEvents} is not implemented due to the fact that only a
     * single observer is needed.
     */
    private class ClickDelegatePanel extends Composite implements Tab {
        private SimplePanel focusablePanel;
        private boolean enabled = true;

        ClickDelegatePanel(Widget child) {
            focusablePanel = new FocusablePanel();
            focusablePanel.setWidget(child);

            initWidget(focusablePanel);

            sinkEvents(Event.ONMOUSEDOWN | Event.ONKEYDOWN);
        }

        @Override
        public HandlerRegistration addClickHandler(ClickHandler handler) {
            return addHandler(handler, ClickEvent.getType());
        }

        @Override
        public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
            return addHandler(handler, KeyDownEvent.getType());
        }

        @Override
        public HandlerRegistration addKeyPressHandler(KeyPressHandler handler) {
            return addDomHandler(handler, KeyPressEvent.getType());
        }

        @Override
        public HandlerRegistration addKeyUpHandler(KeyUpHandler handler) {
            return addDomHandler(handler, KeyUpEvent.getType());
        }

        public SimplePanel getFocusablePanel() {
            return focusablePanel;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEnabled() {
            return enabled;
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

    private static class FocusablePanel extends SimplePanel {
        public FocusablePanel() {
            super(FocusImpl.getFocusImplForPanel().createFocusable());
        }
    }
}
