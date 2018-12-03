package lsfusion.gwt.base.server.spring;

import lsfusion.base.ExternalUtils;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ExternalRequestHandler implements HttpRequestHandler {

    @Autowired
    private BusinessLogicsProvider blProvider;

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String query = request.getQueryString();
            String contentType = request.getContentType();
            ExternalUtils.ExternalResponse responseHttpEntity = ExternalUtils.processRequest(blProvider.getLogics(), request.getRequestURI(),
                    query == null ? "" : query, request.getInputStream(), request.getParameterMap(),
                    contentType != null ? ContentType.create(contentType, request.getCharacterEncoding()) : null);

            if (responseHttpEntity.response != null) {
                response.setContentType(responseHttpEntity.response.getContentType().getValue());
                if(responseHttpEntity.contentDisposition != null)
                    response.addHeader("Content-Disposition", responseHttpEntity.contentDisposition);
                responseHttpEntity.response.writeTo(response.getOutputStream());
            } else
                response.getWriter().print("Executed successfully");

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.addHeader("Content-Type", "text/html; charset=" + request.getCharacterEncoding());
            response.getWriter().print(e.getMessage() != null ? e.getMessage() : "Internal Server Error");
        }
    }
}
