package lsfusion.server.stack;

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
            String resultMessage = ServerResourceBundle.getString(((StackMessage) annotation).value());;

            String paramsString = getArgs(thisJoinPoint, method);
            if (!paramsString.isEmpty()) {
                resultMessage += " : " + paramsString;
            }
            message = resultMessage;
        }
        return message;

    }

    private static String getArgs(ProceedingJoinPoint thisJoinPoint, Method method) {
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

        ImList<String> stringParams = mStringParams.immutableList();
        return stringParams.toString(",");
    }
}
