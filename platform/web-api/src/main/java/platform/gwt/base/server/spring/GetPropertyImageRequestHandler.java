package platform.gwt.base.server.spring;

import com.google.common.io.ByteStreams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class GetPropertyImageRequestHandler implements HttpRequestHandler {
    @Autowired
    private ServletContext context;

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String sid = request.getParameter("sid");

        File imageFile = new File(
                sid.equals("null")
                ? context.getRealPath("images/empty.png")
                : context.getRealPath("WEB-INF/temp/" + sid)
        );

        FileInputStream fis = new FileInputStream(imageFile);
        response.setDateHeader("Expires", System.currentTimeMillis() + 60 * 60 * 24 * 20);
        ByteStreams.copy(fis, response.getOutputStream());
        fis.close();

        if (!sid.equals("null")) {
            imageFile.delete();
        }
    }
}
