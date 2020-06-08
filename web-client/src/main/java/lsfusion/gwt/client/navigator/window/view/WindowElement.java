package lsfusion.gwt.client.navigator.window.view;

import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;

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
    
    public Widget initializeView() {
        pixelWidth = Window.getClientWidth() / 100 * width;
        pixelHeight = Window.getClientHeight() / 100 * height;
        return getView();
    }

    protected void setChildSize(WindowElement child) {}
    public abstract void addElement(WindowElement window);
    public abstract String getCaption();
    public abstract Widget getView();
    
    public abstract String getSID();
    
    public String getStorageSizeKey() {
        return GwtClientUtils.getLogicsName() + "_" + getSID() + "_size";
    }
    
    protected double getPixelHeight() {
        return pixelHeight;
    }

    protected double getPixelWidth() {
        return pixelWidth;
    }

    public void changeInitialSize(int width, int height) {
        if (!sizeStored) {
            if (width > pixelWidth) {
                pixelWidth = width;
            }
            if (height > pixelHeight) {
                pixelHeight = height;
            }
        }
        if (parent != null) {
            parent.setChildSize(this);
        }
    }
}
