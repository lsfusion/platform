package platform.gwt.base.server.handlers;

import com.google.common.io.ByteStreams;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class GetPropertyImageRequestHandler implements HttpRequestHandler {
    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String sid = request.getParameter("sid");
        File imageFile;
        if (!sid.equals("null")) {
            // после перехода на Servlet 3.0 заменить на request.getServletContext().getRealPath()
            imageFile = new File(request.getRealPath("WEB-INF/temp"), sid);
        } else {
            imageFile = new File(request.getRealPath("images"), "empty.png");
        }
        FileInputStream fis = new FileInputStream(imageFile);
        response.setDateHeader("Expires", System.currentTimeMillis() + 60 * 60 * 24 * 20);
        ByteStreams.copy(fis, response.getOutputStream());
        fis.close();
        if (!sid.equals("null")) {
            imageFile.delete();
        }
    }
}
