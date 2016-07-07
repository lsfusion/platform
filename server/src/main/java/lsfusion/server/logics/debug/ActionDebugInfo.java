package lsfusion.server.logics.debug;

public class ActionDebugInfo extends DebugInfo {

    public final ActionDelegationType delegationType;

    public ActionDebugInfo(DebugInfo.DebugPoint point, int debuggerLine, int debuggerOffset, ActionDelegationType type) {
        super(point, debuggerLine, debuggerOffset);    
        delegationType = type;
    }
    
    public ActionDebugInfo(DebugInfo.DebugPoint point, ActionDelegationType type) {
        super(point);
        delegationType = type;
    }

    public ActionDebugInfo(DebugInfo.DebugPoint point, ActionDelegationType type, boolean needToCreateDelegate) {
        this(point, type);
        setNeedToCreateDelegate(needToCreateDelegate);
    }
    
}
