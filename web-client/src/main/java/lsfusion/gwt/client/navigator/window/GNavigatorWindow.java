package lsfusion.gwt.client.navigator.window;

import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.view.GNavigatorView;

import java.util.ArrayList;
import java.util.List;

public class GNavigatorWindow extends GAbstractWindow {
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

    public List<GNavigatorElement> elements = new ArrayList<>();

    public boolean vertical;
    public boolean showSelect;

    public int verticalTextPosition;
    public int horizontalTextPosition;

    public int verticalAlignment;
    public int horizontalAlignment;

    public float alignmentY;
    public float alignmentX;

    public boolean drawScrollBars;

    public boolean isSystem() {
        return canonicalName.equals("System.system");
    }

    public boolean isLogo() {
        return canonicalName.equals("System.logo");
    }

    public boolean isInRootNavBar() {
        return isLogo() || isRoot() || isSystem();
    }

    public boolean isToolbar() {
        return canonicalName.equals("System.toolbar");
    }

    public boolean isVertical() {
        return vertical;
    }

    public boolean isRoot() {
        return canonicalName.equals("System.root");
    }

    public GNavigatorView createView(GINavigatorController navigatorController) {
        return new GNavigatorView(this, navigatorController);
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
