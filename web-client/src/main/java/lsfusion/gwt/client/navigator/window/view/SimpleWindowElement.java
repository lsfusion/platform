package lsfusion.gwt.client.navigator.window.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;

public class SimpleWindowElement extends WindowElement {
    public GAbstractWindow window;

    public SimpleWindowElement(WindowsController controller, GAbstractWindow window, int x, int y, int width, int height) {
        super(controller, x, y, width, height);
        this.window = window;
        controller.registerWindow(window, this);

        if (!GwtSharedUtils.isRedundantString(window.elementClass)) {
            controller.getWindowView(window).addStyleName(window.elementClass);
        }
    }

    @Override
    public void addElement(WindowElement window) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCaption() {
        return window.caption;
    }

    @Override
    public Widget getView() {
        return controller.getWindowView(window);
    }

    @Override
    public boolean isAutoSize(boolean vertical) {
        return window.isAutoSize(vertical);
    }

    @Override
    public String getSID() {
        return window.canonicalName;
    }
}
