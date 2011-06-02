package skolkovo.gwt.login.server;

import org.apache.log4j.Logger;
import platform.gwt.base.server.DebugUtil;
import platform.gwt.base.server.LogicsServiceServlet;
import platform.gwt.base.shared.MessageException;
import skolkovo.api.remote.SkolkovoRemoteInterface;
import skolkovo.gwt.login.client.LoginService;

import java.rmi.RemoteException;

public class LoginServiceImpl extends LogicsServiceServlet<SkolkovoRemoteInterface> implements LoginService {
    protected final static Logger logger = Logger.getLogger(LoginServiceImpl.class);

    @Override
    public void remindPassword(String email) throws MessageException {
        try {
            logics.remindPassword(email);
        } catch (RemoteException e) {
            logger.error("Ошибка в getProfileInfo: ", e);
            e.printStackTrace();
            throw new MessageException(DebugUtil.getInitialCause(e).getMessage());
        }
    }
}