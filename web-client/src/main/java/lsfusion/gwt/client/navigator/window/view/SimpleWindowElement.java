package lsfusion.gwt.client.navigator.window.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;

public class SimpleWindowElement extends WindowElement {
    public GAbstractWindow window;

    public SimpleWindowElement(WindowsController controller, GAbstractWindow window, int x, int y, int width, int height) {
        super(controller, x, y, width, height);
        this.window = window;
    }

    @Override
    public void initializeView(WindowsController controller) {
        controller.registerWindow(window, this);
    }

    @Override
    public void onAddView(WindowsController controller) {
        // here (not in the constructor) because in updateElementClass we need parent
        controller.updateElementClass(window);
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
