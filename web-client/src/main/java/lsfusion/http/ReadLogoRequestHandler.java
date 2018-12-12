package lsfusion.http;

import com.google.common.io.ByteStreams;
import lsfusion.gwt.server.logics.LogicsConnection;
import lsfusion.interop.RemoteLogicsInterface;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

public class ReadLogoRequestHandler extends HttpLogicsRequestHandler {

    @Autowired
    private ServletContext context;

    @Override
    protected void handleRequest(RemoteLogicsInterface remoteLogics, LogicsConnection logicsConnection, HttpServletRequest request, HttpServletResponse response) throws IOException {
        InputStream inputStream;
        byte[] logo = remoteLogics.getGUIPreferences().logicsLogo;
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