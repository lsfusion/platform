package lsfusion.server.stack;

import lsfusion.base.ProgressBar;
import lsfusion.base.col.interfaces.immutable.ImList;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public abstract class ExecutionStackItem {
    protected ProceedingJoinPoint joinPoint;

    public ExecutionStackItem(ProceedingJoinPoint joinPoint) {
        this.joinPoint = joinPoint;
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
