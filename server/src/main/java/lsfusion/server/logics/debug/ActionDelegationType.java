package lsfusion.server.logics.debug;

public enum ActionDelegationType {
    BEFORE_DELEGATE, IN_DELEGATE, AFTER_DELEGATE;
    
    public DebugInfo.DebugPoint getDebugPoint(DebugInfo.DebugPoint startPoint, DebugInfo.DebugPoint endPoint) {
        if (this == BEFORE_DELEGATE) {
            return endPoint;
        } else {
            return startPoint;
        }
    }

}
