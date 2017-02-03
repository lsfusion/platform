package lsfusion.server.stack;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.profiler.AspectProfileObject;
import lsfusion.server.profiler.ProfileObject;
import lsfusion.server.profiler.ProfiledObject;
import lsfusion.server.profiler.Profiler;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

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
        String resultMessage = annotation == null ? "" : ThreadLocalContext.localize(((StackMessage) annotation).value());

        ImList<String> params = getArgs(joinPoint, method);
        if (!params.isEmpty()) {
            resultMessage += " : " + params.toString(",");
        }
        return resultMessage;
    }

    protected static ProfileObject getProfileObject(ProceedingJoinPoint joinPoint, Method method) {
        MethodProfileInfo mpi = getMethodProfileInfo(method);

        Object[] args = joinPoint.getArgs();

        int index = 0;
        Object[] objects = new Object[mpi.size];
        if (mpi.message != null) {
            objects[index++] = mpi.message;
        }
        if (mpi.profileThis) {
            objects[index++] = getProfiledObject(joinPoint.getThis());
        }

        for (int i = 0; i < mpi.profileArgs.length; i++) {
            if (mpi.profileArgs[i]) {
                Object arg = args[i];
                if (arg instanceof ImOrderSet) {
                    objects[index++] = ((ImOrderSet) arg).mapListValues(new GetValue() {
                        @Override
                        public Object getMapValue(Object value) {
                            return getProfiledObject(value);
                        }
                    });
                } else {
                    objects[index++] = getProfiledObject(arg);
                }
            }
        }
        
        return new AspectProfileObject(objects);
    }
    
    private static ConcurrentHashMap<Method, MethodProfileInfo> profileMethodCache = MapFact.getGlobalConcurrentHashMap(); 
    
    private static MethodProfileInfo getMethodProfileInfo(Method method) {
        MethodProfileInfo mpi = profileMethodCache.get(method);
        if (mpi == null) {
            int size = 0;
            Annotation messageAnnotation = method.getAnnotation(StackMessage.class);
            String message = null;
            if (messageAnnotation != null) {
                size++;
                message = ThreadLocalContext.localize(((StackMessage) messageAnnotation).value());
            }

            Annotation thisMessageAnnotation = method.getAnnotation(ThisMessage.class);
            boolean isThis = false;
            if (thisMessageAnnotation != null && ((ThisMessage) thisMessageAnnotation).profile()) {
                size++;
                isThis = true;
            }

            Annotation[][] paramAnnotations = method.getParameterAnnotations();
            boolean[] profileArgs = new boolean[paramAnnotations.length];
            for (int i = 0; i < paramAnnotations.length; i++) {
                profileArgs[i] = false;
                for (Annotation paramAnnotation : paramAnnotations[i]) {
                    if (paramAnnotation instanceof ParamMessage && ((ParamMessage) paramAnnotation).profile()) {
                        size++;
                        profileArgs[i] = true;
                        break;
                    }
                }
            }
            mpi = new MethodProfileInfo(size, message, isThis, profileArgs);
            profileMethodCache.put(method, mpi);
        }
        
        return mpi;
    }
    
    private static Object getProfiledObject(Object object) {
        return object instanceof ProfiledObject ? ((ProfiledObject) object).getProfiledObject() : object;
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
    
    private static class MethodProfileInfo {
        private int size;
        String message;
        boolean profileThis;
        boolean[] profileArgs;
        
        public MethodProfileInfo(int size, String message, boolean profileThis, boolean[] profileArgs) {
            this.size = size;
            this.message = message;
            this.profileThis = profileThis;
            this.profileArgs = profileArgs;
        }
    }
}
