package skolkovo.gwt.login.server;

import org.apache.log4j.Logger;
import skolkovo.gwt.base.server.SkolkovoRemoteServiceServlet;
import skolkovo.gwt.base.shared.MessageException;
import skolkovo.gwt.expert.server.DebugUtil;
import skolkovo.gwt.login.client.LoginService;

import java.rmi.RemoteException;

public class LoginServiceImpl extends SkolkovoRemoteServiceServlet implements LoginService {
    protected final static Logger logger = Logger.getLogger(LoginServiceImpl.class);

    @Override
    public void remindPassword(String email) throws MessageException {
        try {
            getLogics().remindPassword(email);
        } catch (RemoteException e) {
            logger.error("Ошибка в getProfileInfo: ", e);
            e.printStackTrace();
            throw new MessageException(DebugUtil.getInitialCause(e).getMessage());
        }
    }
}