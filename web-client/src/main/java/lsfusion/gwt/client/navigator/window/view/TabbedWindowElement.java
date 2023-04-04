package lsfusion.gwt.client.navigator.window.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.form.design.view.flex.FlexTabbedPanel;

import java.util.ArrayList;
import java.util.List;

public class TabbedWindowElement extends WindowElement {
    private List<WindowElement> children = new ArrayList<>();
    private List<WindowElement> visibleChildren = new ArrayList<>();

    private Panel tabPanel;

    public static class Panel extends FlexTabbedPanel {
        public Panel() {
        }

        @Override
        public void checkResizeEvent(NativeEvent event, Element cursorElement) {
            // do nothing as it clashes with resize in CustomSplitLayoutPanel
        }
    }

    public TabbedWindowElement(WindowsController controller, int x, int y, int width, int height) {
        super(controller, x, y, width, height);

        tabPanel = new Panel();
    }

    public Widget getView() {
        return tabPanel;
    }

    @Override
    public boolean isAutoSize(boolean vertical) {
        for (WindowElement child : children) {
            if (child.isAutoSize(vertical)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addElement(WindowElement element) {
        if (!children.contains(element)) {
            children.add(element);
        }
        element.parent = this;
    }

    @Override
    public String getCaption() {
        return null;
    }

    @Override
    public void initializeView(WindowsController controller) {
        for (WindowElement child : children) {
            child.initializeView(controller);

            Widget windowView = child.getView();

            tabPanel.addTab(windowView, child.getCaption());
            visibleChildren.add(child);
            tabPanel.selectTab(visibleChildren.indexOf(child));
        }
    }

    @Override
    public void onAddView(WindowsController controller) {
        for (WindowElement child : children) {
            child.onAddView(controller);
        }
    }

    @Override
    public void setWindowVisible(WindowElement window) {
        Widget windowView = window.getView();
        if (!visibleChildren.contains(window)) {
            String caption = window.getCaption();
            if (children.indexOf(window) != children.size() - 1 && tabPanel.getWidgetCount() != 0) {
                for (int i = children.indexOf(window) + 1; i < children.size(); i++) {
                    int beforeIndex = visibleChildren.indexOf(children.get(i));
                    if (beforeIndex != -1) {
                        tabPanel.addTab(windowView, beforeIndex, caption);
                        visibleChildren.add(beforeIndex, window);
                        tabPanel.selectTab(beforeIndex);
                        return;
                    }
                }
            }
            
            tabPanel.addTab(windowView, caption);
            visibleChildren.add(window);
            tabPanel.selectTab(visibleChildren.indexOf(window));
            
            if (tabPanel.getTabCount() == 1) {
                setVisible(true);
            }
        }
    }

    @Override
    public void setWindowInvisible(WindowElement window) {
        int index = -1;
        if (visibleChildren.contains(window)) {
            index = visibleChildren.indexOf(window);
            tabPanel.removeTab(index);
            visibleChildren.remove(window);
        }
        
        if (tabPanel.getTabCount() == 0) {
            setVisible(false);
        } else if (index != -1) {
            tabPanel.selectTab(Math.min(index, tabPanel.getTabCount() - 1));
        }
    }

    @Override
    public String getSID() {
        return getSID(children);
    }

    @Override
    public void storeWindowsSizes(Storage storage) {
        for (WindowElement child : children) {
            child.storeWindowsSizes(storage);
        }
    }

    @Override
    public void restoreWindowsSizes(Storage storage) {
        for (WindowElement child : children) {
            child.restoreWindowsSizes(storage);
        }
    }
}
