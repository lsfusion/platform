package lsfusion.gwt.client.navigator.window.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;

import java.util.*;
import java.util.stream.Collectors;

public class FlexWindowElement extends WindowElement {
    private final boolean vertical;
    public List<WindowElement> children = new ArrayList<>();
    private HashMap<WindowElement, Double> flexes = new HashMap<>();
    private HashMap<WindowElement, GSize> prefs = new HashMap<>();
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
                    if (!child.isAutoSize(FlexWindowElement.this.vertical)) {
                        flexes.put(child, ((WidgetLayoutData) layoutData).flex.flex);
                    } else {
                        prefs.put(child, ((WidgetLayoutData) layoutData).flex.flexBasis);
                    }
                }
            }
        };
    }

    @Override
    public void addElement(WindowElement element) {
        if (!children.contains(element)) {
            children.add(element);
            visibleChildren.add(element);
            element.getView().addStyleName(vertical ? "split-vert" : "split-horz");
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
            visibleChildren.add(window);
            redraw(false);
        }
    }

    @Override
    public void setWindowInvisible(WindowElement window) {
        if (visibleChildren.contains(window)) {
            visibleChildren.remove(window);
            redraw(false);
        }
    }
    
    private void redraw(boolean initial) {
        List<WindowElement> orderedChildren = visibleChildren.stream().sorted((o1, o2) -> vertical ? o1.y - o2.y : o1.x - o2.x).collect(Collectors.toList());
        
        int totalSize = 0;
        for (WindowElement child : orderedChildren) {
            if (!child.isAutoSize(vertical)) {
                totalSize += vertical ? child.height : child.width;
            }
        }
        
        // count actual flexes taking into account that some windows may change their visibility in runtime
        double totalFlex = 0d;
        Map<WindowElement, Double> currentFlexes = new HashMap<>();
        for (WindowElement child : orderedChildren) {
            Double flex = flexes.get(child);
            if (flex == null) {
                if (!child.isAutoSize(vertical)) {
                    flex = (double) (vertical ? child.height : child.width) / totalSize;
                } else {
                    flex = 0d;
                }
            } 
            totalFlex += flex;
            currentFlexes.put(child, flex);
        }

        panel.clear();
        for (WindowElement child : orderedChildren) {
            GSize basis = child.isAutoSize(vertical) ? prefs.get(child) : GSize.ZERO;
            Widget windowView = initial ? child.initializeView() : child.getView();
            
            panel.add(windowView, GFlexAlignment.STRETCH, currentFlexes.get(child) / totalFlex, false, basis);
            
            boolean lastInLine = orderedChildren.indexOf(child) == orderedChildren.size() - 1;
            windowView.setStyleName("last-in-line", lastInLine);
        }
    }

    @Override
    public void resetWindowSize() {
        flexes.clear();
        prefs.clear();
        redraw(false);
        
        for (WindowElement child : children) {
            if (child instanceof FlexWindowElement) {
                child.resetWindowSize();
            }
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
