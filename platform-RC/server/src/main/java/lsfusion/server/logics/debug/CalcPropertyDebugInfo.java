package lsfusion.server.logics.debug;

public class CalcPropertyDebugInfo extends DebugInfo {
    public CalcPropertyDebugInfo(DebugInfo.DebugPoint point) {
        super(point);
    }

    public CalcPropertyDebugInfo(DebugInfo.DebugPoint point, int debuggerLine, int debuggerOffset) {
        super(point, debuggerLine, debuggerOffset);
    }

    public CalcPropertyDebugInfo(DebugInfo.DebugPoint point, boolean needToCreateDelegate) {
        this(point);
        setNeedToCreateDelegate(needToCreateDelegate);
    }
}
