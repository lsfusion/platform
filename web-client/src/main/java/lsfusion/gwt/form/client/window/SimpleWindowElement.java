package lsfusion.gwt.form.client.window;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbstractNativeScrollbar;
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
        if (window != null) {
            if (width > initialWidth) {
                // затычка. не позволяем окну расширяться, насколько оно хочет, если это происходит вдоль главной оси
                if (!(window instanceof GTreeNavigatorWindow) &&
                        !(window instanceof GToolbarNavigatorWindow && ((GToolbarNavigatorWindow) window).isHorizontal()) &&
                        !(window instanceof GPanelNavigatorWindow && ((GPanelNavigatorWindow) window).isHorizontal())) {
                    initialWidth = width;
                }

                // если горизональному окну нужно больше места по горизонтали, чем ему предоставили, добавляем к высоте высоту скроллбара
                if ((window instanceof GToolbarNavigatorWindow && ((GToolbarNavigatorWindow) window).isHorizontal()) ||
                        (window instanceof GPanelNavigatorWindow && ((GPanelNavigatorWindow) window).isHorizontal())) {
                    height += AbstractNativeScrollbar.getNativeScrollbarHeight();
                }
            }
            if (height > initialHeight &&
                    !(window instanceof GToolbarNavigatorWindow && ((GToolbarNavigatorWindow) window).isVertical()) &&
                    !(window instanceof GPanelNavigatorWindow && ((GPanelNavigatorWindow) window).isVertical())) {
                initialHeight = height;
            }
        }
        if (parent != null) {
            parent.changeInitialSize(this);
        }
    }
}
