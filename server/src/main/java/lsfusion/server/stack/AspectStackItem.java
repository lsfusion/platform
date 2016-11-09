package lsfusion.server.stack;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.logics.ServerResourceBundle;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class AspectStackItem extends ExecutionStackItem {

    public AspectStackItem(ProceedingJoinPoint joinPoint) {
        super(joinPoint);
    }

    @Override
    public String toString() {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();

        Annotation annotation = method.getAnnotation(StackMessage.class);
        String resultMessage = annotation == null ? "" : ServerResourceBundle.getString(((StackMessage) annotation).value());

        ImList<String> params = getArgs(joinPoint, method);
        if (!params.isEmpty()) {
            resultMessage += " : " + params.toString(",");
        }
        return resultMessage;
    }

    public static ImList<String> getArgs(ProceedingJoinPoint joinPoint, Method method) {
        Object[] args = joinPoint.getArgs();
        MList<String> mStringParams = ListFact.mList();

        Annotation thisMessageAnnotation = method.getAnnotation(ThisMessage.class);
        if (thisMessageAnnotation != null) {
            mStringParams.add(joinPoint.getThis().toString());
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
}
