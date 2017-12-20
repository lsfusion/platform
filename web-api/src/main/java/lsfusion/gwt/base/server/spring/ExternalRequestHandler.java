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
import org.springframework.web.HttpRequestHandler;

import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class ExternalRequestHandler implements HttpRequestHandler {
    private static final String PARAMS_PARAM = "p";
    private static final String RETURNS_PARAM = "returns";

    private static String textPlainType = "text/plain";
    private static String applicationOctetStreamType = "application/octet-stream";
    private static String multipartMixedType = "multipart/mixed";

    public abstract List<Object> processRequest(String property, String[] returns, List<Object> params) throws RemoteException;

    public abstract String getPropertyParam();

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String[] returns = request.getParameterValues(RETURNS_PARAM);
            String[] params = request.getParameterValues(PARAMS_PARAM);
            List<Object> paramsList = params != null ? new ArrayList<Object>(Arrays.asList(params)) : new ArrayList<>();

            String contentType = request.getContentType();
            boolean multipartPost = contentType != null && contentType.contains("multipart");

            byte[] postParams = IOUtils.readBytesFromStream(request.getInputStream());

            if (postParams.length > 0) {
                if (multipartPost) {
                    MimeMultipart multipart = new MimeMultipart(new ByteArrayDataSource(postParams, "multipart/mixed"));
                    for (int i = 0; i < multipart.getCount(); i++) {
                        Object param = multipart.getBodyPart(i).getContent();
                        paramsList.add(param instanceof ByteArrayInputStream ? IOUtils.readBytesFromStream((ByteArrayInputStream) param) : param);
                    }
                } else {
                    paramsList.add(postParams);
                }
            }

            List<Object> returnList = processRequest(request.getParameter(getPropertyParam()), returns, paramsList);

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
                    response.setContentType(multipartMixedType);
                    builder.build().writeTo(response.getOutputStream());
                } else {
                    HttpEntity entity;
                    Object returnEntry = returnList.get(0);
                    if (returnEntry instanceof byte[]) {
                        entity = new ByteArrayEntity((byte[]) returnEntry);
                        response.setContentType(applicationOctetStreamType);
                    } else {
                        entity = new StringEntity((String) returnEntry, ContentType.TEXT_PLAIN);
                        response.setContentType(textPlainType);
                    }

                    entity.writeTo(response.getOutputStream());
                }
            } else {
                response.getWriter().print("Action executed successfully");
            }
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
            throw new RuntimeException(e);
        }
    }
}
