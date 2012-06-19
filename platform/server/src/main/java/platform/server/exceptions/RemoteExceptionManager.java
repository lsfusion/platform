package platform.server.exceptions;

import com.google.common.base.Throwables;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import platform.interop.exceptions.InternalServerException;
import platform.interop.exceptions.LoginException;
import platform.interop.exceptions.RemoteServerException;
import platform.server.Context;
import platform.server.logics.BusinessLogics;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.SQLException;

@Aspect
public class RemoteExceptionManager {
    private final static Logger logger = Logger.getLogger(RemoteExceptionManager.class);
    
    // аспектами ловим все RuntimeException которые доходят до внешней границы сервера и оборачиваем их
    @Around("execution(public * platform.interop.RemoteLogicsInterface.*(..)) ||" +
            "execution(public * platform.interop.navigator.RemoteNavigatorInterface.*(..)) ||" +
            "execution(public * platform.interop.form.RemoteFormInterface.*(..))")
    public Object callRemoteInterface(ProceedingJoinPoint thisJoinPoint) throws Throwable {

        try {
            return thisJoinPoint.proceed();
        } catch (Throwable throwable) {
            if ((throwable instanceof RuntimeException || throwable instanceof AssertionError)
                    && ! (throwable instanceof RemoteServerException)) {
                throw createInternalServerException(throwable);
            } else
                throw throwable;
        }

    }

    public static InternalServerException createInternalServerException(Throwable e) {
        if (!(e.getCause() instanceof LoginException)) {
            e.printStackTrace();
            logger.error("Internal server error: ", e);
            BusinessLogics BL = Context.context.get().getBL();
            try {
                OutputStream os = new ByteArrayOutputStream();
                e.printStackTrace(new PrintStream(os));
                BL.logException(Throwables.getRootCause(e).getMessage(), e.getClass().getName(), os.toString(), null, null, false);
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
        OutputStream os = new ByteArrayOutputStream();
        e.printStackTrace(new PrintStream(os));
        return new InternalServerException(0, e.getLocalizedMessage(), os.toString());
    }
}
