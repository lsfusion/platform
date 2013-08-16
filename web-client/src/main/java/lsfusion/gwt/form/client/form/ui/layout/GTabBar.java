package lsfusion.gwt.form.client.form.ui.layout;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.impl.FocusImpl;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.base.client.ui.GFlexAlignment;

/** based on from com.google.gwt.user.client.ui.TabBar */
public class GTabBar extends Composite implements HasBeforeSelectionHandlers<Integer>, HasSelectionHandlers<Integer> {
    private static final String STYLENAME_DEFAULT = "gwt-TabBarItem";

    public interface Tab extends HasAllKeyHandlers, HasClickHandlers {
    }

    private FlexPanel panel = new FlexPanel(false);

    private Widget selectedTab;

    public GTabBar() {
        initWidget(panel);

        sinkEvents(Event.ONCLICK);

        setStyleName("gwt-TabBar");

        Label first = new Label();
        Label rest = new Label();

        first.setWordWrap(true);
        rest.setWordWrap(true);

        first.setText("\u00A0");
        rest.setText("\u00A0");

        first.setStyleName("gwt-TabBarFirst");
        rest.setStyleName("gwt-TabBarRest");

        panel.add(first, GFlexAlignment.STRETCH, 0);
        panel.add(rest, GFlexAlignment.STRETCH, 1);
    }

    @Override
    public HandlerRegistration addBeforeSelectionHandler(BeforeSelectionHandler<Integer> handler) {
        return addHandler(handler, BeforeSelectionEvent.getType());
    }

    @Override
    public HandlerRegistration addSelectionHandler(SelectionHandler<Integer> handler) {
        return addHandler(handler, SelectionEvent.getType());
    }

    public int getSelectedTab() {
        if (selectedTab == null) {
            return -1;
        }
        return panel.getWidgetIndex(selectedTab) - 1;
    }

    public int getTabCount() {
        return panel.getWidgetCount() - 2;
    }

    public void insertTab(String text, boolean asHTML, int beforeIndex) {
        insertTabWidget(asHTML ? new HTML(text, false) : new Label(text, false), beforeIndex);
    }

    public void insertTab(Widget widget, int beforeIndex) {
        insertTabWidget(widget, beforeIndex);
    }

    public void removeTab(int index) {
        checkTabIndex(index);

        // (index + 1) to account for 'first' placeholder widget.
        Widget toRemove = panel.getWidget(index + 1);
        if (toRemove == selectedTab) {
            selectedTab = null;
        }
        panel.remove(toRemove);
    }

    /**
     * Programmatically selects the specified tab and fires events. Use index -1
     * to specify that no tab should be selected.
     * @param index the index of the tab to be selected
     * @return <code>true</code> if successful, <code>false</code> if the change
     *         is denied by the {@link BeforeSelectionHandler}.
     */
    public boolean selectTab(int index) {
        return selectTab(index, true);
    }

    /**
     * Programmatically selects the specified tab. Use index -1 to specify that no
     * tab should be selected.
     * @param index      the index of the tab to be selected
     * @param fireEvents true to fire events, false not to
     * @return <code>true</code> if successful, <code>false</code> if the change
     *         is denied by the {@link BeforeSelectionHandler}.
     */
    public boolean selectTab(int index, boolean fireEvents) {
        checkTabIndex(index);

        if (fireEvents) {
            BeforeSelectionEvent<?> event = BeforeSelectionEvent.fire(this, index);
            if (event != null && event.isCanceled()) {
                return false;
            }
        }

        // Check for -1.
        setSelectionStyle(selectedTab, false);
        if (index == -1) {
            selectedTab = null;
            return true;
        }

        selectedTab = panel.getWidget(index + 1);
        setSelectionStyle(selectedTab, true);
        if (fireEvents) {
            SelectionEvent.fire(this, index);
        }
        return true;
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
        setStyleName(delPanel.getElement(), "gwt-TabBarItem-disabled", !enabled);
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

        // It is not safe to check if the current widget is an instanceof Label and
        // reuse it here because HTML is an instanceof Label. Leaving an HTML would
        // throw off the results of getTabHTML(int).
        focusablePanel.setWidget(new Label(text, false));
    }

    /**
     * Inserts a new tab at the specified index.
     * @param widget      widget to be used in the new tab
     * @param beforeIndex the index before which this tab will be inserted
     */
    protected void insertTabWidget(Widget widget, int beforeIndex) {
        checkInsertBeforeTabIndex(beforeIndex);

        ClickDelegatePanel delWidget = new ClickDelegatePanel(widget);
        delWidget.setStyleName(STYLENAME_DEFAULT);

        panel.add(delWidget, beforeIndex + 1, GFlexAlignment.STRETCH, 0);

        setStyleName(DOM.getParent(delWidget.getElement()), STYLENAME_DEFAULT + "-wrapper", true);
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
    private boolean selectTabByTabWidget(Widget tabWidget) {
        int numTabs = panel.getWidgetCount() - 1;

        for (int i = 1; i < numTabs; ++i) {
            if (panel.getWidget(i) == tabWidget) {
                return selectTab(i - 1);
            }
        }

        return false;
    }

    private void setSelectionStyle(Widget item, boolean selected) {
        if (item != null) {
            if (selected) {
                item.addStyleName("gwt-TabBarItem-selected");
            } else {
                item.removeStyleName("gwt-TabBarItem-selected");
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

            sinkEvents(Event.ONCLICK | Event.ONKEYDOWN);
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
                case Event.ONCLICK:
                    GTabBar.this.selectTabByTabWidget(this);
                    break;

                case Event.ONKEYDOWN:
                    if (((char) DOM.eventGetKeyCode(event)) == KeyCodes.KEY_ENTER) {
                        GTabBar.this.selectTabByTabWidget(this);
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
