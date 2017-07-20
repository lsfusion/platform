package lsfusion.gwt.form.client.window;

import com.google.gwt.user.client.ui.Widget;

public abstract class WindowElement {
    protected WindowElement parent;
    protected WindowsController main;

    public int x;
    public int y;
    public int width;
    public int height;

    public double initialWidth;
    public double initialHeight;

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

    public void setWindowVisible(WindowElement window) {}
    public void setWindowInvisible(WindowElement window) {}
    protected void changeInitialSize(WindowElement child) {}

    public abstract void addElement(WindowElement window);
    public abstract String getCaption();
    public abstract Widget initializeView();
    public abstract Widget getView();
    
    protected double getInitialHeight() {
        return (main.isFullScreenMode() && parent != null && parent.parent == null) ? 0 : initialHeight;
    }

    protected double getInitialWidth() {
        return (main.isFullScreenMode() && parent != null && parent.parent == null) ? 0 : initialWidth;
    }

    public void changeInitialSize(int width, int height) {
        if (width > initialWidth) {
            initialWidth = width;
        }
        if (height > initialHeight) {
            initialHeight = height;
        }
        if (parent != null) {
            parent.changeInitialSize(this);
        }
    }
}
