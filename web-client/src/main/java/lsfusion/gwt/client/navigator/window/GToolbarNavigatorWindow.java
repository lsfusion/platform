package lsfusion.gwt.client.navigator.window;

import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.view.GNavigatorView;
import lsfusion.gwt.client.navigator.view.GToolbarNavigatorView;

public class GToolbarNavigatorWindow extends GNavigatorWindow {
    public static final float TOP_ALIGNMENT = 0.0f;
    public static final float CENTER_ALIGNMENT = 0.5f;
    public static final float BOTTOM_ALIGNMENT = 1.0f;
    public static final float LEFT_ALIGNMENT = 0.0f;
    public static final float RIGHT_ALIGNMENT = 1.0f;

    public static final int CENTER  = 0;
    public static final int TOP     = 1;
    public static final int LEFT    = 2;
    public static final int BOTTOM  = 3;
    public static final int RIGHT   = 4;

    public boolean vertical;
    public boolean showSelect;

    public int verticalTextPosition;
    public int horizontalTextPosition;

    public int verticalAlignment;
    public int horizontalAlignment;

    public float alignmentY;
    public float alignmentX;

    @Override
    public boolean isVertical() {
        return vertical;
    }

    @Override
    public GNavigatorView createView(GINavigatorController navigatorController) {
        return new GToolbarNavigatorView(this, navigatorController);
    }

    public boolean hasVerticalTextPosition() {
        return verticalTextPosition == BOTTOM;
    }

    public boolean allButtonsActive() {
        return false; //return MainFrame.useBootstrap && !isSystem() && !isRoot() && !isLogo();
    }

    @Override
    public boolean isAutoSize(boolean vertical) {
        if (isVertical() == vertical && !isLogo() && !isSystem()) {
            return false;
        }
        return super.isAutoSize(vertical);
    }
}
