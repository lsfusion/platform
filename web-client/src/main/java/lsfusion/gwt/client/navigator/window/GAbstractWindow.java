package lsfusion.gwt.client.navigator.window;

import lsfusion.gwt.client.base.jsni.HasNativeSID;

import java.io.Serializable;

public class GAbstractWindow implements Serializable, com.google.gwt.user.client.rpc.IsSerializable, HasNativeSID {
    public String canonicalName;
    public String caption;
    public int position;

    public int x;
    public int y;
    public int width;
    public int height;

    // the edge of the page LEFT, RIGHT, TOP or BOTTOM places the window by, instead of POSITION's x, y, width and height:
    // the desktop client's BorderLayout constraint, null for a window POSITION places
    public String borderConstraint;
    public static final String LEFT = "West";
    public static final String RIGHT = "East";
    public static final String TOP = "North";
    public static final String BOTTOM = "South";

    public boolean isByEdge() {
        return borderConstraint != null;
    }

    public boolean titleShown;
    public boolean visible;

    public String elementClass;

    // what draws this window instead of its standard view; what that means depends on the window's role - a navigator
    // window is drawn from its navigator elements, the forms window from the forms open in it, the log window from the
    // messages logged in it
    public String custom;
    public boolean react; // inferred from custom on the server, so the client just reads it
    public boolean single; // one form at a time, drawn alone - what a FORMS window does unless it is TABBED
    // how long the form a new one displaces is kept before it is asked to close, in seconds
    public int closeDelay;
    public static final int NOCLOSE = -1; // ... or not asked at all
    // the window's own orientation: a HORIZONTAL window is a band across, a VERTICAL one a column beside
    public boolean vertical;

    public boolean autoSize;

    @Override
    public String getNativeSID() {
        return canonicalName;
    }

    // System.forms - the layout's centre, the window that carries the platform's toolbar, the one a form falls back to
    // when the window it named was not built, and the only one the mobile layout draws. Read off the name, the way
    // GNavigatorWindow reads isToolbar / isLogo / isRoot / isSystem
    public boolean isSystemForms() {
        return "System.forms".equals(canonicalName);
    }

    // AUTOSIZE hugs the window ACROSS its own orientation and never along it: a horizontal band is as tall as what
    // it draws and still stretches sideways, a vertical panel is as wide as what it draws and still stretches down.
    // A window declares which way it faces, so this is the one rule that says what AUTOSIZE means, and it is the rule
    // GNavigatorWindow used to state for its own windows alone (its override now only carries the exception to it).
    // A window by an edge of the page is as big as what it draws, AUTOSIZE or not, as the desktop client's BorderLayout
    // gives it its preferred size: it has no declared size to take a share of the split along its edge by
    public boolean isAutoSize(boolean vertical) {
        if (isByEdge())
            return true;
        return autoSize && this.vertical != vertical;
    }
}
