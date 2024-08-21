package lsfusion.http.controller.file;

import com.google.common.io.ByteStreams;
import lsfusion.base.BaseUtils;
import lsfusion.gwt.server.FileUtils;
import lsfusion.http.controller.MainController;
import lsfusion.interop.session.ExternalUtils;
import org.apache.commons.httpclient.util.URIUtil;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;

public class DownloadFileRequestHandler implements HttpRequestHandler {

    public DownloadFileRequestHandler() {}

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
        else {
            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            response.setHeader("Location", MainController.getURLPreservingParameters("/exec?action=getResource&p=" + pathInfo.replaceFirst("/", ""), null, request));
            return;
        }
        String fileName = pathInfo.substring(prefix.length());

        String extension = BaseUtils.getFileExtension(fileName);

        String displayName = BaseUtils.getFileName(fileName);

        String version = request.getParameter("version");
        if(version != null)
            fileName = BaseUtils.replaceFileNameAndExtension(fileName, version);

        Charset charset = ExternalUtils.downloadCharset;
        response.setContentType(ExternalUtils.getContentType(extension, charset).toString());
        //inline = open in browser, attachment = download
        response.addHeader("Content-Disposition", "inline; filename*=" + charset.name() + "''" + URIUtil.encodeQuery(getFileName(displayName, extension)));
        // expiration will be set in urlRewrite.xml /file (just to have it at one place)

        // in theory e-tag and last modified may be send but since we're using "version" it's not that necessary

        // it seems that the browser might resend the request (for concurrent reading or whatever)
        FileUtils.readFile(FileUtils.APP_DOWNLOAD_FOLDER_PATH, fileName, !staticFile, true, inStream -> {
            ByteStreams.copy(inStream, response.getOutputStream());
        });
    }

    private String getFileName(String name, String extension) {
        //comma is not allowed in Content-Disposition filename*
        return BaseUtils.getFileName(name, extension).replace(",", "");
    }
}
