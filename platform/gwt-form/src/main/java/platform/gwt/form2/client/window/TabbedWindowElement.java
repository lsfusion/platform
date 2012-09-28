package platform.gwt.form2.client.window;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.form2.shared.view.window.GAbstractWindow;

import java.util.ArrayList;
import java.util.List;

public class TabbedWindowElement extends WindowElement {
    private List<GAbstractWindow> windows = new ArrayList<GAbstractWindow>();

    private List<WindowElement> children = new ArrayList<WindowElement>();

    private TabLayoutPanel panel = new TabLayoutPanel(2, Style.Unit.EM);

    public TabbedWindowElement(WindowContainer main) {
        super(main);
    }

    public List<GAbstractWindow> getWindows() {
        return windows;
    }

    public void addWindow(GAbstractWindow window) {
        windows.add(window);
    }

    public Widget getView() {
        return panel;
    }

    public void addElement(WindowElement element) {
        if (!children.contains(element)) {
            children.add(element);
        }
        element.parent = this;
    }

    @Override
    public void setWindowVisible(WindowElement window) {
        Widget windowView = window.getView();
        if (panel.getWidgetIndex(windowView) == -1) {
            String caption = ((SimpleWindowElement) window).window.caption;
            if (children.indexOf(window) != children.size() - 1 && panel.getWidgetCount() != 0) {
                for (int i = children.indexOf(window) + 1; i < children.size(); i++) {
                    int beforeIndex = panel.getWidgetIndex(children.get(i).getView());
                    if (beforeIndex != -1) {
                        panel.insert(windowView, caption, beforeIndex);
                        return;
                    }
                }
            }
            panel.add(windowView, caption);
            if (panel.getWidgetCount() == 1) {
                setVisible(true);
            }
        }
    }

    @Override
    public void setWindowInvisible(WindowElement window) {
        if (panel.getWidgetIndex(window.getView()) != -1) {
            panel.remove(window.getView());
            if (panel.getWidgetCount() == 0) {
                setVisible(false);
            }
        }
    }
}
