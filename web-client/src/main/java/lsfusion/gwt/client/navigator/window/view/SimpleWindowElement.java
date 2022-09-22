package lsfusion.gwt.client.navigator.window.view;

import com.google.gwt.user.client.ui.AbstractNativeScrollbar;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.navigator.window.GPanelNavigatorWindow;
import lsfusion.gwt.client.navigator.window.GToolbarNavigatorWindow;
import lsfusion.gwt.client.navigator.window.GTreeNavigatorWindow;

public class SimpleWindowElement extends WindowElement {
    public GAbstractWindow window;

    public SimpleWindowElement(WindowsController main, GAbstractWindow window, int x, int y, int width, int height) {
        super(main, x, y, width, height);
        this.window = window;
        main.registerWindow(window, this);

        if (!GwtSharedUtils.isRedundantString(window.elementClass)) {
            main.getWindowView(window).addStyleName(window.elementClass);
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
        return main.getWindowView(window);
    }
    
    @Override
    public void changeInitialSize(int width, int height) {
        if (window != null && !sizeStored) {
            if (width > pixelWidth) {
                // затычка. не позволяем окну расширяться, насколько оно хочет, если это происходит вдоль главной оси
                if (!(window instanceof GTreeNavigatorWindow) &&
                        !(window instanceof GToolbarNavigatorWindow && ((GToolbarNavigatorWindow) window).isHorizontal()) &&
                        !(window instanceof GPanelNavigatorWindow && ((GPanelNavigatorWindow) window).isHorizontal())) {
                    pixelWidth = width;
                }

                // если горизональному окну нужно больше места по горизонтали, чем ему предоставили, добавляем к высоте высоту скроллбара
                height += getScrollbarHeightIfNeeded(width);
            }
            if (height > pixelHeight &&
                    !(window instanceof GToolbarNavigatorWindow && ((GToolbarNavigatorWindow) window).isVertical()) &&
                    !(window instanceof GPanelNavigatorWindow && ((GPanelNavigatorWindow) window).isVertical())) {
                pixelHeight = height;
            }
        }
        if (parent != null) {
            parent.setChildSize(this);
        }
    }
    
    private int getScrollbarHeightIfNeeded(int viewWidth) {
        if (((window instanceof GToolbarNavigatorWindow && ((GToolbarNavigatorWindow) window).isHorizontal()) ||
                (window instanceof GPanelNavigatorWindow && ((GPanelNavigatorWindow) window).isHorizontal())) &&
                viewWidth > getPixelWidth()) {
            return AbstractNativeScrollbar.getNativeScrollbarHeight();
        }
        return 0;
    }

    @Override
    public double getInitialHeight() {
        return super.getInitialHeight() + getScrollbarHeightIfNeeded(getView().getOffsetWidth());
    }

    @Override
    public String getSID() {
        return window.canonicalName;
    }
}
