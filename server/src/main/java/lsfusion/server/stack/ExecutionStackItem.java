package lsfusion.server.stack;

import lsfusion.base.ProgressBar;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.profiler.ProfileObject;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public abstract class ExecutionStackItem {
    protected ProceedingJoinPoint joinPoint;
    protected ProfileObject profileObject;

    public ExecutionStackItem(ProceedingJoinPoint joinPoint, ProfileObject profileObject) {
        this.joinPoint = joinPoint;
        this.profileObject = profileObject;
    }

    public boolean isCancelable() {
        if (joinPoint != null) {
            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            Annotation annotation = method.getAnnotation(Cancelable.class);
            return annotation != null;
        } else return false;
    }

    public ImList<ProgressBar> getProgress() {
        return null;
    }

}
