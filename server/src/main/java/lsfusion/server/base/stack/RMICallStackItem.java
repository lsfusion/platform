package lsfusion.server.base.stack;

import lsfusion.server.physics.admin.profiler.Profiler;
import lsfusion.server.physics.admin.profiler.RMICallProfileObject;
import lsfusion.server.base.remote.context.ContextAwarePendingRemoteObject;
import org.aspectj.lang.ProceedingJoinPoint;

public class RMICallStackItem extends ExecutionStackItem {

    public RMICallStackItem(ProceedingJoinPoint joinPoint, ContextAwarePendingRemoteObject remoteObject) {
        super(joinPoint, Profiler.PROFILER_ENABLED ? new RMICallProfileObject(remoteObject.getProfiledObject(), joinPoint.getSignature().getName()) : null);
    }
    
    @Override
    public String toString() {
        Object remoteObject = joinPoint.getTarget();
        String result = remoteObject + " - RMI ";
        
        result += joinPoint.toShortString();
        return result;
    }
}
