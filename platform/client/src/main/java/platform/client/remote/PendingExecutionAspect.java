package platform.client.remote;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import platform.client.remote.proxy.NonPendingRemoteMethod;
import platform.client.remote.proxy.RemoteObjectProxy;
import platform.interop.remote.MethodInvocation;

import java.lang.reflect.Method;

@Aspect
public class PendingExecutionAspect {
    private static Logger logger = Logger.getLogger(PendingExecutionAspect.class);

    private RemoteObjectProxy lastRemoteObject = null;

    @Around("execution(@platform.client.remote.proxy.PendingRemoteMethod" +
            " * platform.client.remote.proxy.RemoteObjectProxy+.*(..))" +
            " && target(object)")
    public Object moveInvocationToQueue(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        Signature signature = thisJoinPoint.getSignature();
        logger.debug("Pending : " + signature.getDeclaringTypeName() + "." + signature.getName());

        RemoteObjectProxy remoteObject = (RemoteObjectProxy) object;

        if (remoteObject != lastRemoteObject) {
            //если текущий вызов у нового объекта, то выполняем все отложенные методы 
            if (lastRemoteObject != null) {
                lastRemoteObject.flushPendingInvocations();
            }
        }

        Method method = ((MethodSignature) thisJoinPoint.getSignature()).getMethod();
        remoteObject.addPendingInvocation(new MethodInvocation(method.getName(), method.getParameterTypes(), thisJoinPoint.getArgs(), method.getReturnType()));

        lastRemoteObject = remoteObject;
        return null;
    }

    @Around("execution(" +
            "!@(platform.client.remote.proxy.PendingRemoteMethod" +
            " || platform.client.remote.proxy.NonFlushRemoteMethod" +
            " || platform.client.remote.proxy.ImmutableMethod)" +
            " * platform.client.remote.proxy.RemoteObjectProxy+.*(..))" +
            " && target(object)")
    public Object executePendingMethods(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        Signature signature = thisJoinPoint.getSignature();
        logger.debug("Execute all after: " + signature.getDeclaringTypeName() + "." + signature.getName());

        RemoteObjectProxy remoteObject = (RemoteObjectProxy) object;

        if (remoteObject != lastRemoteObject) {
            if (lastRemoteObject != null) {
                RemoteObjectProxy lastRemote = lastRemoteObject;
                lastRemoteObject = null;

                lastRemote.flushPendingInvocations();
            }
            //для удобства определения "подвисания" выполняем и единственный метод нового объекта через очередь
//            return thisJoinPoint.proceed();
        }

        Method method = ((MethodSignature) thisJoinPoint.getSignature()).getMethod();
        Object result = null;
        if (method.getAnnotation(NonPendingRemoteMethod.class) == null) {
            //если отлаживали методы у этого же объекта, то вызываем этот метод вместе с остальными
            logger.debug("  Moving returning method to invocation queue.");
            moveInvocationToQueue(thisJoinPoint, object);
            result = remoteObject.flushPendingInvocations();
        } else {
            //@NonRedirectRemoteMethod показывает, что метод нельзя передавать для выполнения на сервер
            remoteObject.flushPendingInvocations();
            result = thisJoinPoint.proceed();
        }

        lastRemoteObject = null;
        return result;
    }

    @Around("execution(" +
            "@platform.client.remote.proxy.ImmutableMethod" +
            " * platform.client.remote.proxy.RemoteObjectProxy+.*(..))" +
            " && target(object)")
    public Object executeImmutableMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        String name = thisJoinPoint.getSignature().getName();
        RemoteObjectProxy remoteObject = (RemoteObjectProxy) object;
        logger.debug("Running immutable method: " + name);
        if (remoteObject.hasProperty(name)) {
            logger.debug("  Returning cached value: " + remoteObject.getProperty(name));
            return remoteObject.getProperty(name);
        } else {
            logger.debug("  Returning direct value: ");

            //для удобства определения "подвисания" выполняем и этот метод через очередь
            moveInvocationToQueue(thisJoinPoint, object);
            return remoteObject.flushPendingInvocations();
//            return thisJoinPoint.proceed();
        }
    }
}
