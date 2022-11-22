package lsfusion.server.physics.dev.debug;

import lsfusion.server.logics.event.Event;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class PropertyFollowsDebug {
    public final Event event;

    public final boolean isTrue;
    public final DebugInfo.DebugPoint debugPoint;
    public final boolean isFull;

    public final LocalizedString caption;

    public PropertyFollowsDebug(Event event, boolean isTrue, boolean isFull, DebugInfo.DebugPoint debugPoint, LocalizedString caption) {
        this.event = event;

        this.isTrue = isTrue;
        this.isFull = isFull;
        this.debugPoint = debugPoint;

        this.caption = caption;
    }
}
