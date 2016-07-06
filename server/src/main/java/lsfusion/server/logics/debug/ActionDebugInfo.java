package lsfusion.server.logics.debug;

public class ActionDebugInfo extends DebugInfo {

    public final ActionDelegationType delegationType;

    public ActionDebugInfo(DebugInfo info, ActionDelegationType type) {
        super(info.point, info.getDebuggerLine(), info.getDebuggerOffset());
        delegationType = type;
    }

    public ActionDebugInfo(DebugInfo info, ActionDelegationType type, boolean needToCreateDelegate) {
        this(info, type);
        setNeedToCreateDelegate(needToCreateDelegate);
    }
    
}
