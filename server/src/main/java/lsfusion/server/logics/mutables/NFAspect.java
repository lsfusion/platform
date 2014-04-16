package lsfusion.server.logics.mutables;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class NFAspect {

    @Around("execution(@lsfusion.server.logics.mutables.NFLazy * *.*(..)) && target(object)")
    public Object callSynchronizedMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        synchronized (object) {
            return thisJoinPoint.proceed();
        }
    }

}
