package platform.client.remote;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import platform.client.remote.proxy.RemoteObjectProxy;
import platform.interop.remote.MethodInvocation;

import java.lang.reflect.Method;
import java.util.logging.Logger;

@Aspect
public class PendingExecutionAspect {
    private static Logger logger = Logger.getLogger(PendingExecutionAspect.class.getName());

    private RemoteObjectProxy lastRemoteObject = null;

    @Around("execution(@platform.client.remote.proxy.PendingRemoteMethod" +
            " * platform.client.remote.proxy.RemoteObjectProxy+.*(..))" +
            " && target(object)")
    public Object moveInvocationToQueue(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        Signature signature = thisJoinPoint.getSignature();
        logger.finest("Pending : " + signature.getDeclaringTypeName() + "." + signature.getName());

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
            " || platform.client.remote.proxy.NonFlushRemoteMethod)" +
            " * platform.client.remote.proxy.RemoteObjectProxy+.*(..))" +
            " && target(object)")
    public Object executePendingMethods(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        Signature signature = thisJoinPoint.getSignature();
        logger.finest("Execute all after: " + signature.getDeclaringTypeName() + "." + signature.getName());

        RemoteObjectProxy remoteObject = (RemoteObjectProxy) object;

        if (remoteObject != lastRemoteObject) {
            if (lastRemoteObject != null) {
                lastRemoteObject.flushPendingInvocations();
            }
            return thisJoinPoint.proceed();
        }

        //если отлаживали методы у этого же объекта, то вызываем этот метод вместе с остальными
        moveInvocationToQueue(thisJoinPoint, object);

        lastRemoteObject = null;

        return remoteObject.flushPendingInvocations();
    }
}
