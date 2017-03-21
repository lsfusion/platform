package lsfusion.gwt.base.server.spring;

import lsfusion.base.BaseUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ReadLogoRequestHandler implements HttpRequestHandler {

    @Autowired
    private ServletContext context;

    @Autowired
    private BusinessLogicsProvider blProvider;

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        byte[] logo;
        try {
            logo = blProvider.getLogics().getGUIPreferences().logicsLogo;
        } catch (Exception e) {
            logo = IOUtils.toByteArray(context.getResourceAsStream("/splash.jpg"));
        }
        response.setContentType("application/jpg");
        response.addHeader("Content-Disposition", "attachment; filename=logo.jpg");
        response.getOutputStream().write(BaseUtils.getFile(logo));
    }
}