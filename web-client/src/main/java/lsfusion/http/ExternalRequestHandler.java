package lsfusion.http;

import lsfusion.base.ExternalUtils;
import lsfusion.gwt.server.logics.LogicsConnection;
import lsfusion.interop.RemoteLogicsInterface;
import org.apache.http.entity.ContentType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static lsfusion.base.ServerMessages.getString;

public class ExternalRequestHandler extends HttpLogicsRequestHandler {

    @Override
    protected void handleRequest(RemoteLogicsInterface remoteLogics, LogicsConnection logicsConnection, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String query = request.getQueryString();
            String contentTypeString = request.getContentType();
            ContentType contentType = contentTypeString != null ? ContentType.parse(contentTypeString) : null;
            ExternalUtils.ExternalResponse responseHttpEntity = ExternalUtils.processRequest(remoteLogics, request.getRequestURI(),
                    query == null ? "" : query, request.getInputStream(), request.getParameterMap(),
                    contentType);

            if (responseHttpEntity.response != null) {
                response.setContentType(responseHttpEntity.response.getContentType().getValue());
                if(responseHttpEntity.contentDisposition != null)
                    response.addHeader("Content-Disposition", responseHttpEntity.contentDisposition);
                responseHttpEntity.response.writeTo(response.getOutputStream());
            } else
                response.getWriter().print(getString(request, "executed.successfully"));

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("text/html; charset=utf-8");
            response.getWriter().print(getString(request, "internal.server.error.with.message", e.getMessage()));
        }
    }
}
