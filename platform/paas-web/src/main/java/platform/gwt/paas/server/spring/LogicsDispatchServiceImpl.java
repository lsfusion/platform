package platform.gwt.paas.server.spring;

import com.gwtplatform.dispatch.server.Dispatch;
import com.gwtplatform.dispatch.server.RequestProvider;
import com.gwtplatform.dispatch.server.spring.DispatchServiceImpl;
import com.gwtplatform.dispatch.shared.Action;
import com.gwtplatform.dispatch.shared.ActionException;
import com.gwtplatform.dispatch.shared.Result;
import com.gwtplatform.dispatch.shared.ServiceException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import platform.gwt.base.server.LogicsDispatchServlet;
import platform.gwt.base.server.spring.BusinessLogicsProvider;
import platform.gwt.paas.server.exceptions.RemoteActionException;
import platform.gwt.paas.shared.exceptions.InvalidateException;
import platform.gwt.paas.shared.exceptions.MessageException;
import platform.interop.exceptions.RemoteMessageException;

@Component("gwtpDispatch")
public class LogicsDispatchServiceImpl extends DispatchServiceImpl {
    @Autowired
    private BusinessLogicsProvider blProvider;
    protected final static Logger logger = Logger.getLogger(LogicsDispatchServlet.class);

    @Autowired
    public LogicsDispatchServiceImpl(java.util.logging.Logger logger, Dispatch dispatch, RequestProvider requestProvider) {
        super(logger, dispatch, requestProvider);
    }

    @Override
    public Result execute(String cookieSentByRPC, Action<?> action) throws ActionException, ServiceException {
        try {
            //мы никак не юзаем cookieSentByRPC, потому что у нас нету ни одного Action у когторого isSecured == true
            return dispatch.execute(action);
        } catch (RemoteActionException e) {
            blProvider.invalidate();

            logger.error("Ошибка в LogicsDispatchServlet.execute: ", e.getRemote());
            throw new InvalidateException("Внутренняя ошибка сервера. Попробуйте перезагрузить страницу.");
        } catch (RemoteMessageException e) {
            logger.error("Ошибка в LogicsDispatchServlet.execute: ", e);
            throw new MessageException("Внутренняя ошибка сервера: " + e.getMessage());
        } catch (Throwable e) {
            logger.error("Ошибка в LogicsDispatchServlet.execute: ", e);
            throw new MessageException("Внутренняя ошибка сервера.");
        }
    }
}
