package platform.gwt.base.server.handlers;

import com.google.common.io.ByteStreams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.HttpRequestHandler;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class DownloadFileHandler implements HttpRequestHandler {
    @Autowired
    private ServletContext context;

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String fileName = request.getParameter("name");
        File file = new File(context.getRealPath("WEB-INF/temp"), fileName);
        FileInputStream fis = new FileInputStream(file);
        MimetypesFileTypeMap mimeMap;
        try {
            mimeMap = new MimetypesFileTypeMap(context.getRealPath("WEB-INF") + "/mimetypes");
        } catch (IOException e) {
            mimeMap = (MimetypesFileTypeMap) MimetypesFileTypeMap.getDefaultFileTypeMap();
        }
        response.setContentType(mimeMap.getContentType(file));
        response.addHeader("Content-Disposition", "inline; filename=" + fileName);
        ByteStreams.copy(fis, response.getOutputStream());
        fis.close();
        file.delete();
    }
}
