package lsfusion.client.form.design.view.flex;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.base.view.FlexComponent;
import lsfusion.interop.base.view.FlexConstraints;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class FlexTabBar extends JTabbedPane implements FlexComponent, TabBar {

    private Integer selectedTab;

    public FlexTabBar(boolean vertical) {
        super(vertical ? LEFT : TOP, SCROLL_TAB_LAYOUT);

        addChangeListener(e -> {
            if(selectedTab != null) {
                selectTab(getSelectedIndex());
            }
        });
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
        if (selectedTab == null) {
            return -1;
        }
        return selectedTab;
    }

    public void insertTab(String tabText, int beforeIndex) {
        checkInsertBeforeTabIndex(beforeIndex);
        insertTab(tabText, null, null, null, beforeIndex);
    }

    public void removeTab(int index) {
        checkTabIndex(index);

        removeTabAt(index);
    }

    /**
     * Programmatically selects the specified tab and fires events. Use index -1
     * to specify that no tab should be selected.
     * @param index the index of the tab to be selected
     * @return <code>true</code> if successful, <code>false</code> if the change
     *         is denied by the {BeforeSelectionHandler}.
     */
    public boolean selectTab(int index) {
        checkTabIndex(index);

        beforeSelectionHandler.accept(index);

        // Check for -1.
        if (index == -1) {
            selectedTab = null;
            return true;
        }

        selectedTab = index;
        setSelectedIndex(index);

        selectionHandler.accept(index);

        return true;
    }

    /**
     * Sets a tab's text contents.
     * @param index the index of the tab whose text is to be set
     * @param text  the object's new text
     */
    public void setTabText(int index, String text) {
        assert (index >= 0) && (index < getTabCount()) : "Tab index out of bounds";

        setTitleAt(index, text);
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

    @Override
    public Dimension getFlexPreferredSize(Boolean vertical) {
        return getPreferredSize();
    }

    @Override
    public FlexConstraints getFlexConstraints() {
        return new FlexConstraints(FlexAlignment.STRETCH, 0);
    }
}
