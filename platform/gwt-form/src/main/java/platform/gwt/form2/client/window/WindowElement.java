package platform.gwt.form2.client.window;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

public abstract class WindowElement {
    public WindowElement parent;
    protected WindowContainer main;

    public int x;
    public int y;
    public int width;
    public int height;

    public double initialWidth;
    public double initialHeight;
    public boolean initialSizeSet = false;

    public WindowElement(WindowContainer main) {
        this.main = main;
    }

    public void setPosition(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        initialWidth = Window.getClientWidth() / 100 * width;
        initialHeight = Window.getClientHeight() / 100 * height;
    }

    public void setVisible(boolean visible) {
        if (visible) {
            if (parent == null) {
                main.setWindowVisible(this);
            } else {
                parent.setWindowVisible(this);
            }
        } else {
            if (parent == null) {
                main.setWindowInvisible(this);
            } else {
                parent.setWindowInvisible(this);
            }
        }
    }

    public void setWindowVisible(WindowElement window) {}
    public void setWindowInvisible(WindowElement window) {}

    public abstract Widget getView();

    public void changeInitialSize(int width, int height) {
        if (parent == null) {
            if (width > initialWidth) {
                initialWidth = width;
            }
            if (height > initialHeight) {
                initialHeight = height;
            }
            resize();
        }
    }

    // пока подгонка размера работает только для тулбарных и панельных навигаторов, лежащих в главном окне
    private void resize() {
        DockLayoutPanel.Direction direction = main.getWidgetDirection(getView());
        if (direction == DockLayoutPanel.Direction.NORTH || direction == DockLayoutPanel.Direction.SOUTH) {
            main.setWidgetSize(getView(), initialHeight);
        }
        if (direction == DockLayoutPanel.Direction.EAST || direction == DockLayoutPanel.Direction.WEST) {
            main.setWidgetSize(getView(), initialWidth);
        }
    }
}
