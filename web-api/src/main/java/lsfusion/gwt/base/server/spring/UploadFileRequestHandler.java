package lsfusion.gwt.base.server.spring;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class UploadFileRequestHandler implements HttpRequestHandler {
    @Autowired
    private ServletContext context;

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {

            DiskFileItemFactory fileItemFactory = new DiskFileItemFactory();
            ServletFileUpload fileUpload = new ServletFileUpload(fileItemFactory);
            request.setCharacterEncoding("UTF-8");

            List<FileItem> items = fileUpload.parseRequest(request);

            for (FileItem item : items) {
                if (!item.isFormField()) {
                    File file = new File(context.getRealPath("WEB-INF/temp/" +  request.getParameter("sid") + "_" + item.getName()));
                    item.write(file);
                }
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
