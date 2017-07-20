package lsfusion.gwt.form.client.window;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.form.shared.view.window.GAbstractWindow;
import lsfusion.gwt.form.shared.view.window.GPanelNavigatorWindow;
import lsfusion.gwt.form.shared.view.window.GToolbarNavigatorWindow;
import lsfusion.gwt.form.shared.view.window.GTreeNavigatorWindow;

public class SimpleWindowElement extends WindowElement {
    public GAbstractWindow window;

    public SimpleWindowElement(WindowsController main, GAbstractWindow window, int x, int y, int width, int height) {
        super(main, x, y, width, height);
        this.window = window;
        main.registerWindow(window, this);
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
    public Widget initializeView() {
        initialWidth = Window.getClientWidth() / 100 * width;
        initialHeight = Window.getClientHeight() / 100 * height;
        return getView();
    }

    @Override
    public Widget getView() {
        return main.getWindowView(window);
    }

    @Override
    public void changeInitialSize(int width, int height) {
        if (width > initialWidth && 
                (window != null &&  // затычка. не позволяем окну расширяться, насколько оно хочет, если это происходит вдоль главной оси
                    !(window instanceof GTreeNavigatorWindow) && 
                    !(window instanceof GToolbarNavigatorWindow && ((GToolbarNavigatorWindow) window).type == 0)) &&
                    !(window instanceof GPanelNavigatorWindow && ((GPanelNavigatorWindow) window).type == 0)
                ) {
            initialWidth = width;
        }                          
        if (height > initialHeight && 
                (window != null && 
                    !(window instanceof GToolbarNavigatorWindow && ((GToolbarNavigatorWindow) window).type == 1)) &&
                    !(window instanceof GPanelNavigatorWindow && ((GPanelNavigatorWindow) window).type == 1)
                ) {
            initialHeight = height;
        }
        if (parent != null) {
            parent.changeInitialSize(this);
        }
    }
}
