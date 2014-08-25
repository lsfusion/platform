package lsfusion.server.logics.property;

import lsfusion.server.logics.debug.ActionDebugInfo;

public class PropertyFollowsDebug {
    public final boolean isTrue;
    public final ActionDebugInfo debugInfo;

    public PropertyFollowsDebug(boolean isTrue, ActionDebugInfo debugInfo) {
        this.isTrue = isTrue;
        this.debugInfo = debugInfo;
    }
}
