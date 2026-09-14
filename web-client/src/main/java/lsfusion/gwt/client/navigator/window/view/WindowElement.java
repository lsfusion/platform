package lsfusion.gwt.client.navigator.window.view;

import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class WindowElement {
    protected WindowElement parent;
    protected WindowsController controller;

    public int x;
    public int y;
    public int width;
    public int height;

    public WindowElement(WindowsController controller, int x, int y, int width, int height) {
        this.controller = controller;
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
    
    
    public void initializeView(WindowsController controller) {
    }
    public void onAddView(WindowsController controller) {
    }

    public abstract void addElement(WindowElement window);
    public abstract String getCaption();
    public abstract Widget getView();
    public abstract boolean isAutoSize(boolean vertical);

    // a group of windows is sized by its content along an axis only when EVERY window in it is: along the axis a
    // split stacks on, its size is the sum of its children, so one window that wants to grow makes the group have to
    // grow too, and across that axis they are all stretched to the group's size, which a group that hugs has none of
    // to give. The three windows of the standard top strip all hug their height, which is why the strip hugs its own
    // and the work area below it takes the rest; a group holding an AUTOSIZE window BESIDE an ordinary one must not
    protected boolean isAllAutoSize(Collection<WindowElement> windows, boolean vertical) {
        if (windows.isEmpty()) { // vacuously true would make an empty group hug to nothing, which is a change of its own
            return false;
        }
        for (WindowElement child : windows) {
            if (!child.isAutoSize(vertical)) {
                return false;
            }
        }
        return true;
    }

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
    
    // A remembered size is a flex weight or a pixel basis depending on whether the window is content-sized, and the two
    // are not the same number: 20 as a weight is a fifth of the split, 20 as a basis is twenty pixels. So the unit is
    // part of the key. An application that adds or drops AUTOSIZE, or turns a window's orientation, then asks for a key
    // nobody wrote, and the declared size is used - instead of the old number silently read in the wrong unit
    public String getStorageSizeKey(boolean autoSize) {
        return GwtClientUtils.getLogicsName() + "_" + getSID() + (autoSize ? "_px" : "");
    }
}
