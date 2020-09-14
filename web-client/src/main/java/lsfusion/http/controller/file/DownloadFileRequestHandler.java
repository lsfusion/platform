package lsfusion.http.controller.file;

import com.google.common.io.ByteStreams;
import lsfusion.base.BaseUtils;
import lsfusion.base.MIMETypeUtils;
import lsfusion.gwt.server.FileUtils;
import org.apache.commons.httpclient.util.URIUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class DownloadFileRequestHandler implements HttpRequestHandler {
    @Autowired
    private ServletContext context;

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String fileName = request.getParameter("name");
        String displayName = request.getParameter("displayName");
        String extension = request.getParameter("extension");
        boolean actionFile = request.getRequestURI().equals("/downloadFile/action");

        File file = new File(FileUtils.APP_TEMP_FOLDER_URL, fileName);
        
        response.setContentType(MIMETypeUtils.MIMETypeForFileExtension(extension));
        response.addHeader("Content-Disposition", "inline; filename*=UTF-8''" + URIUtil.encodeQuery(getFileName(displayName != null ? displayName : fileName, extension)));
        // expiration will be set in urlRewrite.xml /downloadFile (just to have it at one place)

        try(FileInputStream fis = new FileInputStream(file)) {
            ByteStreams.copy(fis, response.getOutputStream());
        }
        
        if(actionFile)
            FileUtils.deleteFile(file);
    }

    private String getFileName(String name, String extension) {
        //comma is not allowed in Content-Disposition filename*
        return BaseUtils.getFileName(name, extension).replace(",", "");
    }
}
