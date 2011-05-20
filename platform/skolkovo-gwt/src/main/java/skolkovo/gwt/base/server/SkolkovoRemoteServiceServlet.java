package skolkovo.gwt.base.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import platform.base.OSUtils;
import platform.interop.navigator.RemoteNavigatorInterface;
import skolkovo.api.remote.SkolkovoRemoteInterface;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.rmi.RemoteException;

public abstract class SkolkovoRemoteServiceServlet extends RemoteServiceServlet {
    protected SkolkovoRemoteInterface skolkovo;
    protected RemoteNavigatorInterface navigator;
    protected int computerId;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            skolkovo = SkolkovoLogicsClient.getLogics(getServletContext());
            computerId = skolkovo.getComputer(OSUtils.getLocalHostName());
            //todo: make it somethign in the future...
//            navigator = skolkovo.createNavigator("admin", "fusion", computerId);
        } catch (RemoteException e) {
            throw new ServletException("Ошибка инициализации сервлета: ", e);
        }
    }
}
