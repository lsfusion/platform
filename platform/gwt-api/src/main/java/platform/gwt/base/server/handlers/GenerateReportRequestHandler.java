package platform.gwt.base.server.handlers;

import com.google.common.io.ByteStreams;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class GenerateReportRequestHandler implements HttpRequestHandler {
    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        FileInputStream fis = new FileInputStream(new File((String) request.getSession().getAttribute(request.getParameter("file"))));
        String extension = request.getParameter("type");
        response.setContentType("application/" + extension);
        response.addHeader("Content-Disposition", "attachment; filename=" + extension + "Report." + extension);
        ByteStreams.copy(fis, response.getOutputStream());
    }
}
