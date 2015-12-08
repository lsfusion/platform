package lsfusion.server.stack;

import lsfusion.base.ProgressBar;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.logics.ServerResourceBundle;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class AspectStackItem implements ExecutionStackItem {
    private final ProceedingJoinPoint thisJoinPoint;
    private String message;

    public AspectStackItem(ProceedingJoinPoint thisJoinPoint) {
        this.thisJoinPoint = thisJoinPoint;
    }

    @Override
    public String toString() {
        if(message == null) {
            Method method = ((MethodSignature) thisJoinPoint.getSignature()).getMethod();

            Annotation annotation = method.getAnnotation(StackMessage.class);
            String resultMessage = annotation == null ? "" : ServerResourceBundle.getString(((StackMessage) annotation).value());

            ImList<String> params = getArgs(thisJoinPoint, method);
            if (!params.isEmpty()) {
                resultMessage += " : " + params.toString(",");
            }

            ImList<ProgressBar> progressList = getProgress();
            if(!progressList.isEmpty()) {
                resultMessage += ", " + progressList.toString(",");
            }

            message = resultMessage;
        }
        return message;

    }

    public static ImList<String> getArgs(ProceedingJoinPoint thisJoinPoint, Method method) {
        Object[] args = thisJoinPoint.getArgs();
        MList<String> mStringParams = ListFact.mList();

        Annotation thisMessageAnnotation = method.getAnnotation(ThisMessage.class);
        if (thisMessageAnnotation != null) {
            mStringParams.add(thisJoinPoint.getThis().toString());
        }

        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        for(int i=0;i<paramAnnotations.length;i++)
            for(Annotation paramAnnotation : paramAnnotations[i])
                if(paramAnnotation instanceof ParamMessage) {
                    mStringParams.add(args[i].toString());
                    break;
                }

        return mStringParams.immutableList();
    }

    public ImList<ProgressBar> getProgress() {
        Object[] args = thisJoinPoint.getArgs();
        MList<ProgressBar> progressBarList = ListFact.mList();

        Method method = ((MethodSignature) thisJoinPoint.getSignature()).getMethod();
        ImList<String> params = getArgs(thisJoinPoint, method);

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
