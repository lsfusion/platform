package lsfusion.gwt.client.navigator.window;

import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
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

    public int type;
    public boolean showSelect;

    public int verticalTextPosition;
    public int horizontalTextPosition;

    public int verticalAlignment;
    public int horizontalAlignment;

    public float alignmentY;
    public float alignmentX;

    public boolean isVertical() {
        return type == 1;
    }

    public boolean isHorizontal() {
        return type == 0;
    }

    @Override
    public GNavigatorView createView(GINavigatorController navigatorController) {
        return new GToolbarNavigatorView(this, navigatorController);
    }

    public boolean hasVerticalTextPosition() {
        return verticalTextPosition == BOTTOM;
    }
}
