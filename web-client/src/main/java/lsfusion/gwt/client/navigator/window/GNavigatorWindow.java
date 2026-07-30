package lsfusion.gwt.client.navigator.window;

import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.view.GAbstractNavigatorView;
import lsfusion.gwt.client.navigator.view.ToolbarNavigatorView;
import lsfusion.gwt.client.navigator.view.CustomNavigatorView;
import lsfusion.gwt.client.navigator.view.ReactNavigatorView;

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

    // the component name or the HTML template drawing this window's elements instead of the standard toolbar
    public String custom;
    public boolean react; // inferred from custom on the server, so the client just reads it

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

    public boolean isCustom() {
        return custom != null;
    }

    public GAbstractNavigatorView createView(GINavigatorController navigatorController) {
        if (isCustom())
            return react ? new ReactNavigatorView(this, navigatorController) : new CustomNavigatorView(this, navigatorController);

        return new ToolbarNavigatorView(this, navigatorController);
    }

    public boolean hasVerticalTextPosition() {
        return verticalTextPosition == BOTTOM;
    }

    // whether this element's button is drawn in its active state - one rule, asked by the standard strip and by the
    // buttons a custom window hands out
    public boolean isActive(GNavigatorElement element, GNavigatorElement selected) {
        return allButtonsActive() || (element.isFolder() && element.equals(selected));
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
