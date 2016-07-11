package lsfusion.gwt.form.client.window;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.List;

public class TabbedWindowElement extends WindowElement {
    private List<WindowElement> children = new ArrayList<>();

    private TabLayoutPanel tabPanel = new TabLayoutPanel(21, Style.Unit.PX);

    public TabbedWindowElement(WindowsController main, int x, int y, int width, int height) {
        super(main, x, y, width, height);
    }

    public Widget getView() {
        return tabPanel;
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
    public Widget initializeView() {
        for (WindowElement child : children) {
            tabPanel.add(child.initializeView(), child.getCaption());    
        }
        initialWidth = Window.getClientWidth() / 100 * width;
        initialHeight = Window.getClientHeight() / 100 * height;
        return tabPanel;
    }

    @Override
    public void setWindowVisible(WindowElement window) {
        Widget windowView = window.getView();
        if (tabPanel.getWidgetIndex(windowView) == -1) {
            String caption = window.getCaption();
            if (children.indexOf(window) != children.size() - 1 && tabPanel.getWidgetCount() != 0) {
                for (int i = children.indexOf(window) + 1; i < children.size(); i++) {
                    int beforeIndex = tabPanel.getWidgetIndex(children.get(i).getView());
                    if (beforeIndex != -1) {
                        tabPanel.insert(windowView, caption, beforeIndex);
                        return;
                    }
                }
            }
            tabPanel.add(windowView, caption);
            if (tabPanel.getWidgetCount() == 1) {
                setVisible(true);
            }
        }
    }

    @Override
    public void setWindowInvisible(WindowElement window) {
        if (tabPanel.getWidgetIndex(window.getView()) != -1) {
            tabPanel.remove(window.getView());
        }
        if (tabPanel.getWidgetCount() == 0) {
            setVisible(false);
        }
    }
}
