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

    public ProgressStackItem(ProceedingJoinPoint joinPoint) {
        super(joinPoint);
    }

    @Override
    public String toString() {
        String resultMessage = super.toString();

        ImList<ProgressBar> progressList = getProgress();
        if (!progressList.isEmpty()) {
            resultMessage += ", " + progressList.toString(",");
        }

        return resultMessage;
    }

    @Override
    public ImList<ProgressBar> getProgress() {
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
    }
}