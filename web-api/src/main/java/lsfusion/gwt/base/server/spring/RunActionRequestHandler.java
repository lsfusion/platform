package lsfusion.gwt.base.server.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.rmi.RemoteException;

// todo [dale]: Теперь получение свойства осуществляется через каноническое имя, которое содержит различные спецсимволы
// возможно этот код нужно изменить

public class RunActionRequestHandler implements HttpRequestHandler {
    private static final String ACTION_SID_PARAM = "sid";
    private static final String PARAMS_PARAM = "p";

    @Autowired
    private BusinessLogicsProvider blProvider;
    @Autowired
    private ServletContext context;

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String actionSID = request.getParameter(ACTION_SID_PARAM);
            String userLogin = context.getInitParameter("serviceUserLogin");
            if (blProvider.getLogics().checkPropertyChangePermission(userLogin, actionSID)) {
                blProvider.getLogics().runAction(actionSID, request.getParameterValues(PARAMS_PARAM));
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Not enough rights to execute the action");
            }
        } catch(RemoteException e) {
            blProvider.invalidate();
            throw new RuntimeException(e);
        }
        response.getWriter().print("Action executed successfully");
    }
}
