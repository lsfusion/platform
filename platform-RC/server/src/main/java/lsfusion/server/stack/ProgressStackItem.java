package lsfusion.server.stack;

import lsfusion.base.ProgressBar;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.mutable.MList;
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

            ImList<ProgressBar> progressList = getProgress();
            if (!progressList.isEmpty()) {
                resultMessage += ", " + progressList.toString(",");
            }
        } else {
            resultMessage = message + ", " + progress + "of " + total;
        }
        return resultMessage;
    }

    @Override
    public ImList<ProgressBar> getProgress() {
        if (joinPoint != null) {
            Object[] args = joinPoint.getArgs();
            MList<ProgressBar> progressBarList = ListFact.mList();

            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            ImList<String> params = getArgs(joinPoint, method);

            Annotation[][] paramAnnotations = method.getParameterAnnotations();
            for (int i = 0; i < paramAnnotations.length; i++)
                for (Annotation paramAnnotation : paramAnnotations[i])
                    if (paramAnnotation instanceof StackProgress) {
                        ProgressBar progressBar = (ProgressBar) args[i];
                        if (progressBar != null) {
                            String extraParams = params.toString(",");
                            if (!extraParams.isEmpty()) {
                                if (progressBar.params == null)
                                    progressBar.params = extraParams;
                                else
                                    progressBar.params += (progressBar.params.isEmpty() ? "" : ", ") + extraParams;
                            }
                            progressBarList.add(progressBar);
                        }
                    }
            return progressBarList.immutableList();
        } else return ListFact.singleton(new ProgressBar(message, progress, total));
    }
}