package lsfusion.server.base.controller.stack;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.interop.ProgressBar;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class ProgressStackItem extends AspectStackItem {
    private String message;
    private Integer progress;
    private Integer total;

    // не профайлим @StackProgress - передаём null ProfileObject
    public ProgressStackItem(ProceedingJoinPoint joinPoint) {
        super(joinPoint, null);
    }

    public ProgressStackItem(String message, Integer progress, Integer total) {
        super(null, null);
        this.message = message;
        this.progress = progress;
        this.total = total;
    }

    @Override
    public String toString() {
        String resultMessage;
        if (joinPoint != null) {
            resultMessage = super.toString();

            ProgressBar progress = getProgress();
            if (progress != null)
                resultMessage = (resultMessage.isEmpty() ? "" : resultMessage + ", ") + progress;
        } else {
            resultMessage = message + ", " + progress + " of " + total;
        }
        return resultMessage;
    }

    @Override
    public ProgressBar getProgress() {
        if (joinPoint != null) {
            Object[] args = joinPoint.getArgs();

            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            ImList<String> params = getArgs(joinPoint, method);

            Annotation[][] paramAnnotations = method.getParameterAnnotations();
            for (int i = 0; i < paramAnnotations.length; i++)
                for (Annotation paramAnnotation : paramAnnotations[i])
                    if (paramAnnotation instanceof StackProgress) {
                        ProgressBar progressBar = (ProgressBar) args[i];
                        if (progressBar != null) {
                            if(!params.isEmpty())
                                progressBar.params = params.toString(","); 
                            return progressBar;
                        }
                    }
            return null;
        } else 
            return new ProgressBar(message, progress, total);
    }
}