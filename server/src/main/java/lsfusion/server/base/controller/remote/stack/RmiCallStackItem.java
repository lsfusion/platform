package lsfusion.server.base.controller.remote.stack;

import lsfusion.server.base.controller.remote.context.ContextAwarePendingRemoteObject;
import lsfusion.server.base.controller.stack.ExecutionStackItem;
import lsfusion.server.physics.admin.profiler.Profiler;
import lsfusion.server.physics.admin.profiler.RMICallProfileObject;
import org.aspectj.lang.ProceedingJoinPoint;

public class RmiCallStackItem extends ExecutionStackItem {

    public RmiCallStackItem(ProceedingJoinPoint joinPoint, Object profiledObject) {
        super(joinPoint, Profiler.PROFILER_ENABLED ? new RMICallProfileObject(profiledObject, joinPoint.getSignature().getName()) : null);
    }
    
    @Override
    public String toString() {
        Object remoteObject = joinPoint.getTarget();
        String result = remoteObject + " - RMI ";
        
        result += joinPoint.toShortString();
        return result;
    }
}
