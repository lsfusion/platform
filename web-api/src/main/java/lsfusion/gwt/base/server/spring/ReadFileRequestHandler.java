package lsfusion.gwt.base.server.spring;

import lsfusion.base.BaseUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.rmi.RemoteException;

public class ReadFileRequestHandler implements HttpRequestHandler {
    private static final String CN_PARAM = "sid";
    private static final String PARAMS_PARAM = "p";

    @Autowired
    private BusinessLogicsProvider blProvider;
    @Autowired
    private ServletContext context;

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String user = context.getInitParameter("serviceUserLogin");
        String propertyCN = request.getParameter(CN_PARAM);

        try {
            byte[] file = blProvider.getLogics().readFile(propertyCN, request.getParameterValues(PARAMS_PARAM));
            if (file != null) {
                String extension = BaseUtils.getExtension(file);
                response.setContentType("application/" + extension);
                response.addHeader("Content-Disposition", "attachment; filename=" + propertyCN.replace(',', '_') + "." + extension);
                response.getOutputStream().write(BaseUtils.getFile(file));
            } else {
                response.getWriter().print("null");
            }
        } catch(RemoteException e) {
            blProvider.invalidate();
            throw new RuntimeException(e);
        }
    }
}
