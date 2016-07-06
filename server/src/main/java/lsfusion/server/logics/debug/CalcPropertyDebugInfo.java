package lsfusion.server.logics.debug;

public class CalcPropertyDebugInfo extends DebugInfo {
    public CalcPropertyDebugInfo(DebugInfo info) {
        super(info.point, info.getDebuggerLine(), info.getDebuggerOffset());
    }

    public CalcPropertyDebugInfo(DebugInfo info, boolean needToCreateDelegate) {
        this(info);
        setNeedToCreateDelegate(needToCreateDelegate);
    }
}
