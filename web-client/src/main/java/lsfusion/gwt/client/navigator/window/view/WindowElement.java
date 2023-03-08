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
    
    
    public Widget initializeView(WindowsController controller) {
        return getView();
    }
    public void onAddView(WindowsController controller) {
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
    
    public String getStorageSizeKey(boolean flex) {
        return GwtClientUtils.getLogicsName() + "_" + getSID() + "_" + (flex ? "flex" : "basis");
    }
}
