package lsfusion.server.base.controller.remote;

import lsfusion.base.ExceptionUtils;
import lsfusion.interop.base.exception.RemoteInternalException;
import lsfusion.interop.base.exception.RemoteMessageException;
import lsfusion.interop.base.exception.RemoteServerException;
import lsfusion.server.base.controller.remote.context.ContextAwarePendingRemoteObject;
import lsfusion.server.base.controller.remote.context.RemoteContextAspect;
import lsfusion.server.base.controller.remote.manager.RmiServer;
import lsfusion.server.base.controller.stack.ThrowableWithStack;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.stack.NewThreadExecutionStack;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.rmi.RemoteException;

@Aspect
public class RemoteExceptionsAspect {
    private final static Logger logger = ServerLoggers.systemLogger;

    // аспектами ловим все RuntimeException которые доходят до внешней границы сервера и оборачиваем их
    @Around(RemoteContextAspect.allRemoteCalls)
    public Object executeRemoteMethod(ProceedingJoinPoint thisJoinPoint, Object target) throws Throwable {
        try {
            boolean interrupted = Thread.interrupted();
            ServerLoggers.assertLog(!interrupted, "RMI THREAD SHOULD NOT BE NOT INTERRUPTED AT THIS POINT");

            return thisJoinPoint.proceed();
        } catch (Throwable throwable) {
            boolean suppressLog = throwable instanceof RemoteInternalException; // "nested remote call" so we don't need to log it twice
            if(throwable instanceof ThreadDeath || ExceptionUtils.getRootCause(throwable) instanceof InterruptedException) {
                logger.error("Thread '" + Thread.currentThread() + "' was forcefully stopped.");
                suppressLog = true; // we don't need that situation, because if client ran some really long action and exited, all his threads will be stopped eventually, and then we'll get a lot of that exceptions
            }

            throwable = fromAppServerToWebServerAndDesktopClient(throwable);

            if(!suppressLog)
                logException(throwable, target);
            
            throw throwable;
        } finally {
            Thread.interrupted(); // dropping interrupted flag, otherwise it will go with the thread to the next rmi call
        }
    }

    public void logException(Throwable throwable, Object target) {
        if(throwable instanceof RemoteInternalException) {                                
            try {
                BusinessLogics businessLogics = ThreadLocalContext.getBusinessLogics();
                if(target instanceof ContextAwarePendingRemoteObject)
                    ThreadLocalContext.assureRmi((ContextAwarePendingRemoteObject)target);
                else
                    ThreadLocalContext.assureRmi((RmiServer) target);
                NewThreadExecutionStack stack = ThreadLocalContext.getStack();
                businessLogics.systemEventsLM.logException(businessLogics, stack, throwable, null, null, false, false);
            } catch (Throwable t) {
                logger.error("Error when logging exception: ", t);
            }
        }
    }

    // result throwable class should exist both on web-server and on desktop-client
    private static Throwable fromAppServerToWebServerAndDesktopClient(Throwable e) {
        // this classes exist both on web-server and desktop-client and unlikely to have causes
        if(e instanceof RemoteException || e instanceof RemoteServerException)
            return e;

        ThrowableWithStack throwableWithStack = new ThrowableWithStack(e);

        Throwable throwable = throwableWithStack.getThrowable();
        if(throwableWithStack.isNoStackRequired())
            return new RemoteMessageException(throwable.getMessage());

        RemoteInternalException result = new RemoteInternalException(ThreadLocalContext.localize("{exceptions.internal.server.error}")
                                    + ": " + ExceptionUtils.copyMessage(throwable), throwableWithStack.getLsfStack());
        ExceptionUtils.copyStackTraces(throwable, result);
        return result;
    }
}
