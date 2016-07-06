package lsfusion.server.logics.debug;

public enum ActionDelegationType {
    BEFORE_DELEGATE, IN_DELEGATE, AFTER_DELEGATE;
    
    public ActionDebugInfo getDebugInfo(DebugInfo startInfo, DebugInfo endInfo) {
        if (this == BEFORE_DELEGATE) {
            return new ActionDebugInfo(endInfo, this);
        } else {
            return new ActionDebugInfo(startInfo, this);
        }
    }

}
