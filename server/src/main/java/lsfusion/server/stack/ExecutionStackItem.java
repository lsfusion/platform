package lsfusion.server.stack;

import lsfusion.base.ProgressBar;
import lsfusion.base.col.interfaces.immutable.ImList;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public abstract class ExecutionStackItem {
    protected ProceedingJoinPoint joinPoint;
    public Integer processId;

    public ExecutionStackItem(ProceedingJoinPoint joinPoint) {
        this.joinPoint = joinPoint;
        this.processId = (int) Thread.currentThread().getId();
    }

    public boolean isCancelable() {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Annotation annotation = method.getAnnotation(Cancelable.class);
        return  annotation != null;
    }

    public ImList<ProgressBar> getProgress() {
        return null;
    }

}
