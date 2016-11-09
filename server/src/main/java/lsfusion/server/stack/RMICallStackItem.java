package lsfusion.server.stack;

import lsfusion.server.remote.RemoteForm;
import org.aspectj.lang.ProceedingJoinPoint;

public class RMICallStackItem extends ExecutionStackItem {

    public RMICallStackItem(ProceedingJoinPoint joinPoint) {
        super(joinPoint);
    }
    
    @Override
    public String toString() {
        RemoteForm remoteForm = (RemoteForm) joinPoint.getTarget();
        String result = remoteForm + " - RMI ";
        
        result += joinPoint.toShortString();
        return result;
    }
}
