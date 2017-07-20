package lsfusion.server.stack;

import lsfusion.server.profiler.Profiler;
import lsfusion.server.profiler.RMICallProfileObject;
import lsfusion.server.remote.RemoteForm;
import org.aspectj.lang.ProceedingJoinPoint;

public class RMICallStackItem extends ExecutionStackItem {

    public RMICallStackItem(ProceedingJoinPoint joinPoint) {
        super(joinPoint, Profiler.PROFILER_ENABLED ? new RMICallProfileObject((((RemoteForm) joinPoint.getTarget()).form.entity), joinPoint.getSignature().getName()) : null);
    }
    
    @Override
    public String toString() {
        RemoteForm remoteForm = (RemoteForm) joinPoint.getTarget();
        String result = remoteForm + " - RMI ";
        
        result += joinPoint.toShortString();
        return result;
    }
}
