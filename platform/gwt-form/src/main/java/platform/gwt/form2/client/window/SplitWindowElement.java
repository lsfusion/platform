package platform.gwt.form2.client.window;

import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.*;

public class SplitWindowElement extends WindowElement {
    private boolean vertical;

    // with visibility
    private LinkedHashMap<WindowElement, Boolean> children = new LinkedHashMap<WindowElement, Boolean>();

    private SplitLayoutPanel panel = new SplitLayoutPanel();

    public SplitWindowElement(WindowContainer main, boolean vertical) {
        super(main);
        this.vertical = vertical;
    }

    public void addElement(WindowElement element) {
        if (!children.containsKey(element)) {
            children.put(element, true);
        }
        element.parent = this;

        List<WindowElement> sortedList = new ArrayList<WindowElement>(children.keySet());
        Collections.sort(sortedList, COORDINATES_COMPARATOR);
        LinkedHashMap<WindowElement, Boolean> sortedMap = new LinkedHashMap<WindowElement, Boolean>();
        for (WindowElement window : sortedList) {
            sortedMap.put(window, children.get(window));
        }
        children = sortedMap;
    }

    public Widget getView() {
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
        children.put(window, true);
        if (panel.getWidgetIndex(window.getView()) == -1) {
            redraw();
        }
    }

    @Override
    public void setWindowInvisible(WindowElement window) {
        children.put(window, false);
        if (panel.getWidgetIndex(window.getView()) != -1) {
            redraw();
        }
    }

    private void redraw() {
        panel.clear();
        ArrayList<WindowElement> visibleWindows = new ArrayList<WindowElement>();
        for (WindowElement window : children.keySet()) {
            if (children.get(window)) {
                visibleWindows.add(window);
            }
        }
        if (!visibleWindows.isEmpty()) {
            for (int i = 0; i < visibleWindows.size() - 1; i++) {
                WindowElement toAdd = visibleWindows.get(i);
                if (vertical) {
                    panel.addNorth(toAdd.getView(), toAdd.initialHeight);
                } else {
                    panel.addWest(toAdd.getView(), toAdd.initialWidth);
                }
            }
            panel.add(visibleWindows.get(visibleWindows.size() - 1).getView());
        }
        setVisible(panel.getWidgetCount() > 0);
    }
}
