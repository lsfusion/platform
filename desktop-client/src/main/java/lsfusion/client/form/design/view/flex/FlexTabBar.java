package lsfusion.client.form.design.view.flex;

import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.form.design.view.FlexPanel;
import lsfusion.client.form.design.view.widget.LabelWidget;
import lsfusion.client.form.design.view.widget.Widget;
import lsfusion.interop.base.view.FlexAlignment;

import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/** based on from com.google.gwt.user.client.ui.TabBar */
public class FlexTabBar extends FlexPanel/*Composite*/ implements TabBar {

    public interface Tab/* extends HasAllKeyHandlers, HasClickHandlers*/ {
    }

    private final FlexPanel panel;

    private Widget selectedTab;

    public FlexTabBar(Widget extraTabWidget, boolean vertical) {
        super(vertical);
        panel = new FlexPanel(vertical);
        if (extraTabWidget == null) {
            initWidget(panel);
        } else {
            FlexPanel tabBarContainer = new FlexPanel(vertical);
            tabBarContainer.addFill(panel);
            tabBarContainer.add(extraTabWidget);
            initWidget(tabBarContainer);
        }

        // first is to have an offset on the left, rest not sure what for
        LabelWidget first = new LabelWidget();
        LabelWidget rest = new LabelWidget();

        first.setText("\u00A0");
        rest.setText("\u00A0");

        panel.add((Widget) first, FlexAlignment.STRETCH);
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
        if (selectedTab == null) {
            return -1;
        }
        return panel.getWidgetIndex(selectedTab) - 1;
    }

    public int getTabCount() {
        return panel.getComponentCount() - 2;
    }

    public void insertTab(Widget widget, int beforeIndex) {
        checkInsertBeforeTabIndex(beforeIndex);

        //todo: as gwt-TabBarItem. We need also outside emptyBorder as margin, but it is shown with same background as component
        //widget.getComponent().setBorder(new CompoundBorder(new CompoundBorder(new EmptyBorder(0, 0, 0, 5), new LineBorder(SwingDefaults.getPanelBorderColor())), new EmptyBorder(2, 5, 2, 5)));
        widget.getComponent().setBorder(new CompoundBorder(new LineBorder(SwingDefaults.getPanelBorderColor()), new EmptyBorder(2, 5, 2, 5)));

        panel.add(widget, beforeIndex + 1, FlexAlignment.STRETCH);

        //added instead of onBrowserEvent
        widget.getComponent().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                selectTabByTabWidget(widget);
                widget.getComponent().setCursor(null);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                if(e.getComponent() != selectedTab) {
                    widget.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    setHoverStyle(widget, true);
                }

            }

            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e);
                if(e.getComponent() != selectedTab) {
                    widget.getComponent().setCursor(null);
                    setHoverStyle(widget, false);
                }
            }
        });
    }

    @Override
    public Widget asWidget() {
        return null;
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
     *         is denied by the {BeforeSelectionHandler}.
     */
    public boolean selectTab(int index) {
        checkTabIndex(index);

        beforeSelectionHandler.accept(index);

        // Check for -1.
        setSelectionStyle(selectedTab, false);
        if (index == -1) {
            selectedTab = null;
            return true;
        }

        selectedTab = panel.getWidget(index + 1);
        setSelectionStyle(selectedTab, true);

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

        Widget widget = panel.getWidget(index + 1);

        widget.getComponent().add(new LabelWidget(text));
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
        int numTabs = panel.getComponentCount() - 1;

        for (int i = 1; i < numTabs; ++i) {
            if (panel.getComponent(i) == tabWidget) {
                return selectTab(i - 1);
            }
        }

        return false;
    }

    private void setSelectionStyle(Widget item, boolean selected) {
        if (item != null) {
            if (selected) {
                item.getComponent().setOpaque(true);
                item.getComponent().setBackground(SwingDefaults.getSelectionColor());
            } else {
                item.getComponent().setOpaque(false);
                item.getComponent().setBackground(null);
            }
        }
    }

    public void setHoverStyle(Widget item, boolean entered) {
        if (item != null) {
            if (entered) {
                item.getComponent().setOpaque(true);
                item.getComponent().setBackground(SwingDefaults.getButtonPressedBackground());
            } else {
                item.getComponent().setOpaque(false);
                item.getComponent().setBackground(null);
            }
        }
    }

    private void initWidget(FlexPanel panel) {
        add((Widget) panel);
    }
}
