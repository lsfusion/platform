package lsfusion.gwt.client.navigator.window.view;

import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class WindowElement {
    protected WindowElement parent;
    protected WindowsController main;

    public int x;
    public int y;
    public int width;
    public int height;

    public double pixelWidth;
    public double pixelHeight;
    public boolean sizeStored = false;
    public boolean initialSizeSet = false;
    
    public boolean autoSize;

    public WindowElement(WindowsController main, int x, int y, int width, int height) {
        this.main = main;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setVisible(boolean visible) {
        if (parent != null) {
            if (visible) {
                parent.setWindowVisible(this);
            } else {
                parent.setWindowInvisible(this);
            }
        }
    }
    
    public void storeWindowsSizes(Storage storage) {}
    public void restoreWindowsSizes(Storage storage) {}

    public void setWindowVisible(WindowElement window) {}
    public void setWindowInvisible(WindowElement window) {}
    
    protected double initialWidth;
    protected double initialHeight;
    
    public Widget initializeView() {
        pixelWidth = Window.getClientWidth() / 100 * width;
        pixelHeight = Window.getClientHeight() / 100 * height;
        initialWidth = pixelWidth;
        initialHeight = pixelHeight;
        return getView();
    }

    public abstract void addElement(WindowElement window);
    public abstract String getCaption();
    public abstract Widget getView();
    public abstract boolean isAutoSize(boolean vertical);

    public abstract String getSID();

    protected String getSID(Collection<WindowElement> windows) {
        List<String> childrenSIDs = new ArrayList<>();
        for (WindowElement child : windows) {
            childrenSIDs.add(child.getSID());
        }
        
        childrenSIDs.sort(String.CASE_INSENSITIVE_ORDER);

        StringBuilder sid = new StringBuilder();
        for (String childSID : childrenSIDs) {
            sid.append(childSID);
            if (childrenSIDs.indexOf(childSID) < childrenSIDs.size() - 1) {
                sid.append("_");
            }
        }
        return sid.toString();
    }
    
    public String getStorageSizeKey() {
        return GwtClientUtils.getLogicsName() + "_" + getSID() + "_size";
    }

    public int getAutoSize(boolean vertical) {
        return vertical ? getView().getOffsetHeight() : getView().getOffsetWidth();
    }
    
    protected double getPixelHeight() {
        return pixelHeight;
    }

    protected double getPixelWidth() {
        return pixelWidth;
    }

    public void resetWindowSize() {
        sizeStored = false;
        initialSizeSet = false;
    }

    public void changePixelSize(boolean vertical, int size) {
        if (vertical) {
            pixelHeight = size;
        } else {
            pixelWidth = size;
        }
    }
}
