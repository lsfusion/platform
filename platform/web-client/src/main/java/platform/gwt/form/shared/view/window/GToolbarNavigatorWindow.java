package platform.gwt.form.shared.view.window;

import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import platform.gwt.form.client.navigator.GINavigatorController;
import platform.gwt.form.client.navigator.GNavigatorView;
import platform.gwt.form.client.navigator.GToolbarNavigatorView;

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
    public static final int LEADING  = 10;
    public static final int TRAILING = 11;

    public int type;
    public boolean showSelect;

    public int verticalTextPosition;
    public int horizontalTextPosition;

    public int verticalAlignment;
    public int horizontalAlignment;

    public float alignmentY;
    public float alignmentX;

    @Override
    public GNavigatorView createView(GINavigatorController navigatorController) {
        return new GToolbarNavigatorView(this, navigatorController);
    }

    public HasVerticalAlignment.VerticalAlignmentConstant getAlignmentY() {
        if (alignmentY != 0) {
            if (alignmentY == TOP_ALIGNMENT) {
                return HasVerticalAlignment.ALIGN_TOP;
            } else if (alignmentY == CENTER_ALIGNMENT) {
                return HasVerticalAlignment.ALIGN_MIDDLE;
            } else if (alignmentY == BOTTOM_ALIGNMENT) {
                return HasVerticalAlignment.ALIGN_BOTTOM;
            }
        }
        return HasVerticalAlignment.ALIGN_TOP;
    }

    public HasAlignment.HorizontalAlignmentConstant getAlignmentX() {
        if (alignmentX != 0) {
            if (alignmentX == LEFT_ALIGNMENT) {
                return HasAlignment.ALIGN_LEFT;
            } else if (alignmentX == CENTER_ALIGNMENT) {
                return HasAlignment.ALIGN_CENTER;
            } else if (alignmentX == RIGHT_ALIGNMENT) {
                return HasAlignment.ALIGN_RIGHT;
            }
        }
        return HasAlignment.ALIGN_LEFT;
    }

    public boolean hasVerticalTextPosition() {
        return verticalTextPosition == BOTTOM;
    }
}
