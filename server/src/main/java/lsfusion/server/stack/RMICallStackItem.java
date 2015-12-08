package lsfusion.server.stack;

import lsfusion.server.remote.RemoteForm;
import org.aspectj.lang.ProceedingJoinPoint;

public class RMICallStackItem implements ExecutionStackItem {
    private ProceedingJoinPoint joinPoint;

    public RMICallStackItem(ProceedingJoinPoint joinPoint) {
        this.joinPoint = joinPoint;
    }
    
    @Override
    public String toString() {
        RemoteForm remoteForm = (RemoteForm) joinPoint.getTarget();
        String result = remoteForm.getCanonicalName() + " - RMI ";
        
        result += joinPoint.toShortString();
        return result;
    }
}
