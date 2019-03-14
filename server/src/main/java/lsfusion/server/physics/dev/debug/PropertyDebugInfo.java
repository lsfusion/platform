package lsfusion.server.physics.dev.debug;

public class PropertyDebugInfo extends DebugInfo {
    public PropertyDebugInfo(DebugInfo.DebugPoint point) {
        super(point);
    }

    public PropertyDebugInfo(DebugInfo.DebugPoint point, int debuggerLine, int debuggerOffset) {
        super(point, debuggerLine, debuggerOffset);
    }

    public PropertyDebugInfo(DebugInfo.DebugPoint point, boolean needToCreateDelegate) {
        this(point);
        setNeedToCreateDelegate(needToCreateDelegate);
    }
}
