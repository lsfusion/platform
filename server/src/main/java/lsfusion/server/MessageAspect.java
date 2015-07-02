package lsfusion.server;

import org.aspectj.lang.annotation.Aspect;

@Aspect
public class MessageAspect {

//    @Around("execution(@lsfusion.server.stack.Message * *.*(..))")
//    public Object callTwinMethod(ProceedingJoinPoint thisJoinPoint) throws Throwable {
//        Method method = ((MethodSignature) thisJoinPoint.getSignature()).getMethod();
//        Object[] args = thisJoinPoint.getArgs();
//
//        ImList<String> stringParams = getArgs(thisJoinPoint, method, args);
//
//        for(Annotation annotation : method.getAnnotations())
//            if(annotation instanceof Message) {
//                String message = ServerResourceBundle.getString(((Message) annotation).value());
//                if(stringParams.size() > 0)
//                    message = message + " : " + stringParams.toString(",");
//                Object result = null;
//                ThreadLocalContext.pushActionMessage(message);
//                try {
//                    result = thisJoinPoint.proceed();
//                } finally {
//                    ThreadLocalContext.popActionMessage();
//                }
//                return result;
//            }
//        throw new RuntimeException("wrong aspect");
//    }
//
//    public static ImList<String> getArgs(ProceedingJoinPoint thisJoinPoint, Method method, Object[] args) {
//        MList<String> stringParams = ListFact.mList();
//
//        for(Annotation annotation : method.getAnnotations())
//            if(annotation instanceof ThisMessage) {
//                stringParams.add(thisJoinPoint.getThis().toString());
//            }
//
//        Annotation[][] paramAnnotations = method.getParameterAnnotations();
//        for(int i=0;i<paramAnnotations.length;i++)
//            for(Annotation paramAnnotation : paramAnnotations[i])
//                if(paramAnnotation instanceof ParamMessage) {
//                    stringParams.add(args[i].toString());
//                    break;
//                }
//        return stringParams.immutableList();
//    }

}
