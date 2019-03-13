package lsfusion.server.physics.dev.debug;

public class ActionDebugInfo extends DebugInfo {

    public final ActionDelegationType delegationType;

    public ActionDebugInfo(DebugInfo.DebugPoint point, Integer debuggerLine, Integer debuggerOffset, ActionDelegationType type) {
        super(point, debuggerLine, debuggerOffset);    
        delegationType = type;
    }
    
    public ActionDebugInfo(DebugInfo.DebugPoint point, ActionDelegationType type, boolean needToCreateDelegate) {
        this(point, null, null, type);
        setNeedToCreateDelegate(needToCreateDelegate);
    }
    
}
