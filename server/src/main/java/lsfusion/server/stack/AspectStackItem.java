package lsfusion.server.stack;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.logics.ServerResourceBundle;
import lsfusion.server.profiler.AspectProfileObject;
import lsfusion.server.profiler.ProfileObject;
import lsfusion.server.profiler.ProfiledObject;
import lsfusion.server.profiler.Profiler;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class AspectStackItem extends ExecutionStackItem {

    public AspectStackItem(ProceedingJoinPoint joinPoint) {
        this(joinPoint, Profiler.PROFILER_ENABLED ? getProfileObject(joinPoint, ((MethodSignature) joinPoint.getSignature()).getMethod()) : null);
    }

    public AspectStackItem(ProceedingJoinPoint joinPoint, ProfileObject profileObject) {
        super(joinPoint, profileObject);
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

    protected static ProfileObject getProfileObject(ProceedingJoinPoint joinPoint, Method method) {
        MList<Object> objects = ListFact.mList();
        
        Annotation annotation = method.getAnnotation(StackMessage.class);
        if (annotation != null) {
            objects.add(ServerResourceBundle.getString(((StackMessage) annotation).value()));
        }

        Annotation thisMessageAnnotation = method.getAnnotation(ThisMessage.class);
        if (thisMessageAnnotation != null && ((ThisMessage) thisMessageAnnotation).profile()) {
            Object obj = joinPoint.getThis();
            objects.add(obj instanceof ProfiledObject ? ((ProfiledObject) obj).getProfiledObject() : obj);
        }

        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < paramAnnotations.length; i++) {
            for (Annotation paramAnnotation : paramAnnotations[i]) {
                if (paramAnnotation instanceof ParamMessage && ((ParamMessage) paramAnnotation).profile()) {
                    Object arg = args[i];
                    objects.add(arg instanceof ProfiledObject ? ((ProfiledObject) arg).getProfiledObject() : arg);
                }
            }
        }
        
        return objects.size() > 0 ? new AspectProfileObject(objects.immutableList().toArray(new Object[objects.size()])) : null;
    }

    public static ImList<String> getArgs(ProceedingJoinPoint joinPoint, Method method) {
        Object[] args = joinPoint.getArgs();
        MList<String> mStringParams = ListFact.mList();

        Annotation thisMessageAnnotation = method.getAnnotation(ThisMessage.class);
        if (thisMessageAnnotation != null) {
            mStringParams.add(joinPoint.getThis().toString());
        }

        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < paramAnnotations.length; i++)
            for (Annotation paramAnnotation : paramAnnotations[i])
                if (paramAnnotation instanceof ParamMessage) {
                    mStringParams.add(args[i].toString());
                    break;
                }

        return mStringParams.immutableList();
    }
}
