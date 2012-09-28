package platform.gwt.form2.client.window;

import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SplitWindowElement extends WindowElement {
    private boolean vertical;

    private List<WindowElement> children = new ArrayList<WindowElement>();

    private SplitLayoutPanel panel = new SplitLayoutPanel();

    public SplitWindowElement(WindowContainer main, boolean vertical) {
        super(main);
        this.vertical = vertical;
    }

    public void addElement(WindowElement element) {
        if (!children.contains(element)) {
            children.add(element);
        }
        element.parent = this;
    }

    public Widget getView() {
        Collections.sort(children, COORDINATES_COMPARATOR);
        return panel;
    }

    private Comparator<WindowElement> COORDINATES_COMPARATOR = new Comparator<WindowElement>() {
        public int compare(WindowElement c1, WindowElement c2) {
            if (vertical) {
                return c1.y > c2.y ? 1 : -1;
            } else {
                return c1.x > c2.x ? 1 : -1;
            }
        }
    };

    @Override
    public void setWindowVisible(WindowElement window) {
        Widget windowView = window.getView();
        if (panel.getWidgetIndex(windowView) == -1) {
            if (children.indexOf(window) == children.size() - 1) {
                panel.add(windowView);
            } else {
                for (int i = children.indexOf(window) + 1; i < children.size(); i++) {
                    Widget beforeWidget = children.get(i).getView();
                    if (panel.getWidgetIndex(beforeWidget) != -1) {
                        if (vertical) {
                            panel.insert(windowView, DockLayoutPanel.Direction.NORTH, window.initialHeight, beforeWidget);
                        } else {
                            panel.insert(windowView, DockLayoutPanel.Direction.WEST, window.initialWidth, beforeWidget);
                        }
                        if (panel.getWidgetCount() == 1) {
                            setVisible(true);
                        }
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void setWindowInvisible(WindowElement window) {
        if (panel.getWidgetIndex(window.getView()) != -1) {
            panel.remove(window.getView());
            panel.forceLayout();
            if (panel.getWidgetCount() == 0) {
                setVisible(false);
            }
        }
    }
}
