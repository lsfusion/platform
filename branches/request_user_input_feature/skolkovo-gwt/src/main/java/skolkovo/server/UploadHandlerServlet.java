package skolkovo.server;

import gwtupload.server.UploadAction;
import gwtupload.server.exceptions.UploadActionException;
import org.apache.commons.fileupload.FileItem;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;

/**
 * todo...
 */
public class UploadHandlerServlet extends UploadAction {

    private static final long serialVersionUID = 1L;

    Hashtable<String, String> receivedContentTypes = new Hashtable<String, String>();
    Hashtable<String, File> receivedFiles = new Hashtable<String, File>();

    @Override
    public String executeAction(HttpServletRequest request, List<FileItem> sessionFiles) throws UploadActionException {
        String response = "";
        int cont = 0;
        for (FileItem item : sessionFiles) {
            item.get();
            if (!item.isFormField()) {
                cont++;
                try {
                    File file = File.createTempFile("upload-", ".bin");
                    item.write(file);

                    receivedFiles.put(item.getFieldName(), file);
                    receivedContentTypes.put(item.getFieldName(), item.getContentType());

                    response += "File saved as " + file.getAbsolutePath();
                } catch (Exception e) {
                    throw new UploadActionException(e);
                }
            }
        }

        removeSessionFileItems(request);

        return response;
    }

    @Override
    public void getUploadedFile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String fieldName = request.getParameter(PARAM_SHOW);
        File f = receivedFiles.get(fieldName);
        if (f != null) {
            response.setContentType(receivedContentTypes.get(fieldName));
            FileInputStream is = new FileInputStream(f);
            copyFromInputStreamToOutputStream(is, response.getOutputStream());
        } else {
            renderXmlResponse(request, response, ERROR_ITEM_NOT_FOUND);
        }
    }

    @Override
    public void removeItem(HttpServletRequest request, String fieldName) throws UploadActionException {
        File file = receivedFiles.get(fieldName);
        receivedFiles.remove(fieldName);
        receivedContentTypes.remove(fieldName);
        if (file != null) {
            file.delete();
        }
    }
}