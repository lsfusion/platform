package lsfusion.gwt.base.server.spring;

import lsfusion.base.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class EvalRequestHandler implements HttpRequestHandler {
    private static final String SCRIPT_PARAM = "script";
    private static final String PARAMS_PARAM = "p";
    private static final String RETURNS_PARAM = "returns";

    @Autowired
    private BusinessLogicsProvider blProvider;
    @Autowired
    private ServletContext context;

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String script = request.getParameter(SCRIPT_PARAM);
            String[] returns = request.getParameterValues(RETURNS_PARAM);
            String[] getParams = request.getParameterValues(PARAMS_PARAM);
            byte[] postParams = IOUtils.readBytesFromStream(request.getInputStream());
            List<Object> returnList = blProvider.getLogics().eval(returns, script, postParams, getParams);

            if (!returnList.isEmpty()) {
                if (returnList.size() > 1) {
                    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                    builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                    builder.setContentType(ContentType.MULTIPART_FORM_DATA);
                    for (int i = 0; i < returnList.size(); i++) {
                        Object returnEntry = returnList.get(i);
                        if (returnEntry instanceof byte[])
                            builder.addPart("param" + i, new ByteArrayBody((byte[]) returnEntry, returns[i]));
                        else
                            builder.addPart("param" + i, new StringBody((String) returnEntry, ContentType.TEXT_HTML));
                    }
                    response.setContentType("multipart/mixed");
                    builder.build().writeTo(response.getOutputStream());
                } else {
                    HttpEntity entity;
                    Object returnEntry = returnList.get(0);
                    if (returnEntry instanceof byte[])
                        entity = new ByteArrayEntity((byte[]) returnEntry);
                    else
                        entity = new StringEntity((String) returnEntry, ContentType.TEXT_HTML);
                    response.setContentType("text/plain");
                    entity.writeTo(response.getOutputStream());
                }
            } else {
                response.getWriter().print("Script executed successfully");
            }

        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
            throw new RuntimeException(e);
        }
    }
}
