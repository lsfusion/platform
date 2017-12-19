package lsfusion.gwt.base.server.spring;

import org.springframework.beans.factory.annotation.Autowired;

import java.rmi.RemoteException;
import java.util.List;

public class ExecRequestHandler extends ExternalRequestHandler {
    private static final String ACTION_CN_PARAM = "action";

    @Autowired
    private BusinessLogicsProvider blProvider;

    @Override
    public List<Object> processRequest(String actionCN, String[] returns, List<Object> params) throws RemoteException {
        return blProvider.getLogics().exec(actionCN, returns, params.toArray());
    }

    @Override
    public String getPropertyParam() {
        return ACTION_CN_PARAM;
    }
}
