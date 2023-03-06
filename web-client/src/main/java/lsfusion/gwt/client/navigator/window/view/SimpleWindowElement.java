package lsfusion.gwt.client.navigator.window.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;

public class SimpleWindowElement extends WindowElement {
    public GAbstractWindow window;

    public SimpleWindowElement(WindowsController main, GAbstractWindow window, int x, int y, int width, int height) {
        super(main, x, y, width, height);
        this.window = window;
        main.registerWindow(window, this);

        main.updateElementClass(window);
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
        return main.getWindowView(window);
    }

    @Override
    public boolean isAutoSize() {
        return window.autoSize;
    }

    @Override
    public int getAutoSize(boolean vertical) {
        if (window instanceof GNavigatorWindow) {
            return main.getNavigatorView((GNavigatorWindow) window).getAutoSize(vertical);
        }
        return super.getAutoSize(vertical);
    }

    @Override
    public String getSID() {
        return window.canonicalName;
    }
}
