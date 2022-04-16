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
        boolean staticFile = request.getRequestURI().equals("/downloadFile/static");

        response.setContentType(MIMETypeUtils.MIMETypeForFileExtension(extension));
        //inline = open in browser, attachment = download
        response.addHeader("Content-Disposition", "inline; filename*=UTF-8''" + URIUtil.encodeQuery(getFileName(displayName != null ? displayName : fileName, extension)));
        // expiration will be set in urlRewrite.xml /downloadFile (just to have it at one place)

        FileUtils.readFile(FileUtils.APP_DOWNLOAD_FOLDER_PATH, fileName, !staticFile, inStream -> {
            ByteStreams.copy(inStream, response.getOutputStream());
        });
    }

    private String getFileName(String name, String extension) {
        //comma is not allowed in Content-Disposition filename*
        return BaseUtils.getFileName(name, extension).replace(",", "");
    }
}
