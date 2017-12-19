package lsfusion.gwt.base.server.spring;

import org.springframework.beans.factory.annotation.Autowired;

import java.rmi.RemoteException;
import java.util.List;

public class EvalRequestHandler extends ExternalRequestHandler {
    private static final String SCRIPT_PARAM = "script";

    @Autowired
    private BusinessLogicsProvider blProvider;

    @Override
    public List<Object> processRequest(String property, String[] returns, List<Object> params) throws RemoteException {
        return blProvider.getLogics().eval(property, returns, params.toArray());
    }

    @Override
    public String getPropertyParam() {
        return SCRIPT_PARAM;
    }
}
