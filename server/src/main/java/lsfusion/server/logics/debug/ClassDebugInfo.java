package lsfusion.server.logics.debug;

public class ClassDebugInfo extends DebugInfo {
    public ClassDebugInfo(DebugPoint debugPoint, int debuggerLine, int debuggerOffset) {
        super(debugPoint, debuggerLine, debuggerOffset);
    }

    public ClassDebugInfo(DebugPoint point) {
        super(point);
    }
}
