package platform.gwt.paas.server.spring;

import com.gwtplatform.dispatch.server.Dispatch;
import com.gwtplatform.dispatch.server.RequestProvider;
import com.gwtplatform.dispatch.server.spring.DispatchServiceImpl;
import com.gwtplatform.dispatch.shared.Action;
import com.gwtplatform.dispatch.shared.ActionException;
import com.gwtplatform.dispatch.shared.Result;
import com.gwtplatform.dispatch.shared.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import platform.base.ExceptionUtils;
import platform.gwt.base.server.LogicsDispatchServlet;
import platform.gwt.base.server.spring.BusinessLogicsProvider;
import platform.gwt.paas.server.exceptions.RemoteActionException;
import platform.gwt.paas.shared.exceptions.MessageException;

import java.util.logging.Logger;

@Component("gwtpDispatch")
public class LogicsDispatchServiceImpl extends DispatchServiceImpl {
    @Autowired
    private BusinessLogicsProvider blProvider;
    protected final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(LogicsDispatchServlet.class);

    @Autowired
    public LogicsDispatchServiceImpl(Logger logger, Dispatch dispatch, RequestProvider requestProvider) {
        super(logger, dispatch, requestProvider);
    }

    @Override
    public Result execute(String cookieSentByRPC, Action<?> action) throws ActionException, ServiceException {
        try {
            return super.execute(cookieSentByRPC, action);
        } catch (RemoteActionException e) {
            String errorMessage = null;
            if (!ExceptionUtils.isRecoverableRemoteException(e.getRemote())) {
                blProvider.invalidate();
                errorMessage = "Внутренняя ошибка сервера. Попробуйте перезагрузить страницу.";
            } else {
                errorMessage = ExceptionUtils.getInitialCause(e.getRemote()).getMessage();
            }

            e.printStackTrace();
            logger.error("Ошибка в LogicsDispatchServiceImpl.execute: ", e);
            throw new MessageException(errorMessage);
        } catch (Throwable e) {
            e.printStackTrace();
            logger.error("Ошибка в LogicsDispatchServiceImpl.execute: ", e);
            throw new MessageException("Внутренняя ошибка сервера.");
        }
    }
}
