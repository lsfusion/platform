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

    public String borderConstraint;

    public boolean titleShown;
    public boolean visible;

    public String elementClass;

    // what draws this window instead of its standard view; what that means depends on the window's role - a navigator
    // window is drawn from its navigator elements, the forms window from the forms open in it, the log window from the
    // messages logged in it
    public String custom;
    public boolean react; // inferred from custom on the server, so the client just reads it
    public boolean single; // one form at a time, drawn alone - what a FORMS window does unless it is TABBED

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

    public boolean isAutoSize(boolean vertical) {
        return autoSize;
    }
}
