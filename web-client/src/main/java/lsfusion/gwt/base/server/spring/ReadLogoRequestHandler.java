package lsfusion.gwt.base.server.spring;

import com.google.common.io.ByteStreams;
import lsfusion.gwt.form.server.logics.spring.LogicsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

public class ReadLogoRequestHandler implements HttpRequestHandler {

    @Autowired
    private ServletContext context;

    @Autowired
    private LogicsProvider blProvider;

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        byte[] logo = null;
        InputStream inputStream;
        try {
            logo = blProvider.getLogics().getGUIPreferences().logicsLogo;
        } catch (Exception e) {
            blProvider.invalidate();
        }
        if (logo == null) {
            inputStream = new FileInputStream(new File(context.getRealPath("images/logo.png")));
        } else {
            inputStream = new ByteArrayInputStream(logo);
        }
        response.setContentType("image/jpg");
        response.addHeader("Content-Disposition", "attachment; filename=logo.jpg");
        ByteStreams.copy(inputStream, response.getOutputStream());
    }
}