package lsfusion.gwt.client.navigator.window.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;

import java.util.*;

public class FlexWindowElement extends WindowElement {
    private final boolean vertical;
    public List<WindowElement> children = new ArrayList<>();
    private HashMap<WindowElement, Double> flexes = new HashMap<>();
    private Set<WindowElement> visibleChildren = new HashSet<>();
    private FlexPanel panel;
    
    public FlexWindowElement(boolean vertical, WindowsController main, int x, int y, int width, int height) {
        super(main, x, y, width, height);
        this.vertical = vertical;
        panel = new FlexPanel(vertical, GFlexAlignment.STRETCH) {
            @Override
            public void onResize() {
                super.onResize();
                for (WindowElement child : visibleChildren) {
                    Object layoutData = child.getView().getLayoutData();
                    flexes.put(child, ((WidgetLayoutData) layoutData).flex.flex);
                }
            }
        };
    }

    @Override
    public void addElement(WindowElement element) {
        if (!children.contains(element)) {
            children.add(element);
            visibleChildren.add(element);
        }
        element.parent = this;
    }

    @Override
    public Widget initializeView() {
        redraw(true);
        return super.initializeView();
    }

    @Override
    public void setWindowVisible(WindowElement window) {
        if (!visibleChildren.contains(window)) {
            flexes.clear();
            visibleChildren.add(window);
            redraw(false);
        }
    }

    @Override
    public void setWindowInvisible(WindowElement window) {
        if (visibleChildren.contains(window)) {
            flexes.clear();
            visibleChildren.remove(window);
            redraw(false);
        }
    }
    
    private void redraw(boolean initial) {
        panel.clear();
        List<WindowElement> toSort = new ArrayList<>();
        int totalSize = 0;
        for (WindowElement windowElement : visibleChildren) {
            toSort.add(windowElement);
            if (!windowElement.isAutoSize(vertical)) {
                totalSize += vertical ? windowElement.height : windowElement.width;
            }
        }
        toSort.sort((o1, o2) -> vertical ? o1.y - o2.y : o1.x - o2.x);
        
        for (WindowElement windowElement : toSort) {
            Double flex = flexes.get(windowElement);
            boolean autoSize = windowElement.isAutoSize(vertical);
            if (flex == null) {
                if (!autoSize) {
                    flex = (double) (vertical ? windowElement.height : windowElement.width) / totalSize;
                } else {
                    flex = 0d;
                }
            } 
            GSize basis = autoSize ? null : GSize.ZERO;
            panel.add(initial ? windowElement.initializeView() : windowElement.getView(), GFlexAlignment.STRETCH, flex, false, basis);
//            flexes.put(windowElement, flex);
        }
    }

    public void setBorderWindowsHidden(boolean hidden) {
        // assume that 'forms' window is always added in the end of container and its parents - in the end of their containers recursively 
        WindowElement lastChild = children.get(children.size() - 1);
        if (hidden) {
            panel.clear();
            panel.add(lastChild.getView(), GFlexAlignment.STRETCH, 1, false, GSize.ZERO);
        } else {
            redraw(false);
        }
        if (lastChild instanceof FlexWindowElement) {
            ((FlexWindowElement) lastChild).setBorderWindowsHidden(hidden);
        }
    }

    @Override
    public String getCaption() {
        StringBuilder caption = new StringBuilder();
        for (Iterator<WindowElement> iterator = children.iterator(); iterator.hasNext(); ) {
            String childCaption = iterator.next().getCaption();
            if (childCaption != null) {
                caption.append(childCaption);
                if (iterator.hasNext()) {
                    caption.append("/");
                }
            }
        }
        return (caption.length() == 0) ? null : caption.toString();
    }

    @Override
    public Widget getView() {
        return panel;
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
    public String getSID() {
        return getSID(children);
    }
}
