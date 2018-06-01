package lsfusion.server.stack;

import lsfusion.server.profiler.Profiler;
import lsfusion.server.profiler.RMICallProfileObject;
import lsfusion.server.remote.ContextAwarePendingRemoteObject;
import lsfusion.server.remote.RemoteForm;
import org.aspectj.lang.ProceedingJoinPoint;

import java.util.Arrays;

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
