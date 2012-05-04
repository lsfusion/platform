package platform.gwt.base.server.handlers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestHandler;
import platform.base.BaseUtils;
import platform.gwt.base.server.spring.BusinessLogicsProvider;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.StringTokenizer;

@Component("readFileHandler")
public class ReadFileRequestHandler implements HttpRequestHandler {
    private static final String SID_PARAM = "sid";
    private static final String PARAMS_PARAM = "params";

    @Autowired
    private BusinessLogicsProvider blProvider;

    private void doReadFile(String propertySid, String paramsString, HttpServletResponse httpServletResponse) throws IOException {
        StringTokenizer tokenizer = new StringTokenizer(paramsString, "_");
        String[] params = new String[tokenizer.countTokens()];
        int i = 0;
        while (tokenizer.hasMoreElements()) {
            params[i++] = tokenizer.nextToken();
        }
        byte[] file = blProvider.getLogics().readFile(propertySid, params);
        String extension = BaseUtils.getExtension(file);
        httpServletResponse.setContentType("application/" + extension);
        httpServletResponse.addHeader("Content-Disposition", "attachment; filename=" + propertySid + "." + extension);
        httpServletResponse.getOutputStream().write(BaseUtils.getFile(file));
    }

    @Override
    public void handleRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        String user = httpServletRequest.getRemoteUser();
        String propertySid = httpServletRequest.getParameter(SID_PARAM);
        if (user == null) { //предполагается, что сработал PropertyReadAccessFilter и у свойства стоит по дефолту разрешение на чтение
            doReadFile(propertySid, httpServletRequest.getParameter(PARAMS_PARAM), httpServletResponse);
            return;
        }

        boolean permission = blProvider.getLogics().checkPropertyViewPermission(user, propertySid);
        if (!permission) {
            httpServletResponse.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "File load is forbidden");
        } else {
            doReadFile(propertySid, httpServletRequest.getParameter(PARAMS_PARAM), httpServletResponse);
        }
    }
}
