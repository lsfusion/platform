package lsfusion.gwt.client.navigator.window.view;

import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;

import java.util.*;
import java.util.stream.Collectors;

public class FlexWindowElement extends WindowElement {
    private final boolean vertical;
    public List<WindowElement> children = new ArrayList<>();
    private Panel panel;

    public static class Panel extends FlexPanel {
        public Panel(boolean vertical, GFlexAlignment flexAlignment) {
            super(vertical, flexAlignment);
        }
    }
    
    public FlexWindowElement(boolean vertical, WindowsController controller, int x, int y, int width, int height) {
        super(controller, x, y, width, height);
        this.vertical = vertical;
        panel = new Panel(vertical, GFlexAlignment.STRETCH);
    }

    @Override
    public void addElement(WindowElement element) {
        children.add(element);
        element.parent = this;
        element.getView().addStyleName(vertical ? "split-vert" : "split-horz");
    }

    @Override
    public void initializeView(WindowsController controller) {
        List<WindowElement> orderedChildren = getOrderedChildren();

        for (WindowElement child : orderedChildren) {
            child.initializeView(controller);

            boolean autoSize = child.isAutoSize(vertical);
            panel.add(child.getView(), GFlexAlignment.STRETCH, autoSize ? 0.0 : (vertical ? child.height : child.width), false, autoSize ? null : GSize.ZERO);
        }
    }

    @Override
    public void onAddView(WindowsController controller) {
        for (WindowElement child : children)
            child.onAddView(controller);
    }

    @Override
    public void setWindowVisible(WindowElement window) {
        window.getView().setVisible(true);
    }

    @Override
    public void setWindowInvisible(WindowElement window) {
        window.getView().setVisible(false);
    }

    private List<WindowElement> getOrderedChildren() {
        return children.stream().sorted((o1, o2) -> vertical ? o1.y - o2.y : o1.x - o2.x).collect(Collectors.toList());
    }

    public void resetWindowSize() {
        for (WindowElement child : children) {
            FlexPanel.FlexLayoutData flexLayoutData = ((FlexPanel.WidgetLayoutData) child.getView().getLayoutData()).flex;

            panel.setFlex(child.getView(), flexLayoutData.baseFlex, flexLayoutData.baseFlexBasis);

            if (child instanceof FlexWindowElement) {
                ((FlexWindowElement) child).resetWindowSize();
            }
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

    @Override
    public void storeWindowsSizes(Storage storage) {
        for (WindowElement child : children) {
            FlexPanel.FlexLayoutData flexLayoutData = ((FlexPanel.WidgetLayoutData) child.getView().getLayoutData()).flex;

            String storedSize;
            if (child.isAutoSize(vertical)) {
                storedSize = flexLayoutData.flexBasis != null ? flexLayoutData.flexBasis.getResizeSize().toString() : null;
            } else {
                storedSize = String.valueOf(flexLayoutData.flex);
            }
            storage.setItem(child.getStorageSizeKey(), storedSize);
            
            child.storeWindowsSizes(storage);
        }
    }
    
    @Override
    public void restoreWindowsSizes(Storage storage) {
        for (WindowElement child : children) {
            String sizeString = storage.getItem(child.getStorageSizeKey());

            Double storedSize = sizeString != null && !sizeString.equals("null") ? Double.valueOf(sizeString) : null; // it seems that somewhy sizeString can be null
            if (storedSize != null) {
                boolean autoSize = child.isAutoSize(vertical);
                panel.setFlex(child.getView(), autoSize ? 0.0 : storedSize, autoSize ? GSize.getResizeNSize((int) Math.round(storedSize)) : GSize.ZERO);
            }

            child.restoreWindowsSizes(storage);
        }
    }
}
