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

        String pathInfo = request.getPathInfo();

        boolean staticFile;
        String prefix;
        if(pathInfo.startsWith(prefix = "/" + FileUtils.STATIC_PATH + "/"))
            staticFile = true;
        else if(pathInfo.startsWith(prefix = "/" + FileUtils.TEMP_PATH + "/"))
            staticFile = false;
        else if(pathInfo.startsWith(prefix = "/" + FileUtils.DEV_PATH + "/"))
            staticFile = true;
        else
            throw new UnsupportedOperationException("Path info : " + pathInfo);
        String fileName = pathInfo.substring(prefix.length());

        String extension = request.getParameter("extension");
        if(extension == null)
            extension = BaseUtils.getFileExtension(fileName);

        String displayName = request.getParameter("displayName");
        if(displayName == null)
            displayName = BaseUtils.getFileName(fileName);

        String version = request.getParameter("version");
        if(version != null)
            fileName = BaseUtils.replaceFileNameAndExtension(fileName, version);

        response.setContentType(MIMETypeUtils.MIMETypeForFileExtension(extension));
        //inline = open in browser, attachment = download
        response.addHeader("Content-Disposition", "inline; filename*=UTF-8''" + URIUtil.encodeQuery(getFileName(displayName, extension)));
        // expiration will be set in urlRewrite.xml /downloadFile (just to have it at one place)

        // in theory e-tag and last modified may be send but since we're using "version" it's not that necessary

        FileUtils.readFile(FileUtils.APP_DOWNLOAD_FOLDER_PATH, fileName, !staticFile, inStream -> {
            ByteStreams.copy(inStream, response.getOutputStream());
        });
    }

    private String getFileName(String name, String extension) {
        //comma is not allowed in Content-Disposition filename*
        return BaseUtils.getFileName(name, extension).replace(",", "");
    }
}
