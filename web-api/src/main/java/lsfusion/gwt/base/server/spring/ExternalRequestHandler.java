package lsfusion.gwt.base.server.spring;

import lsfusion.base.ExternalUtils;
import org.apache.http.HttpEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;

public class ExternalRequestHandler implements HttpRequestHandler {

    @Autowired
    private BusinessLogicsProvider blProvider;

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String query = request.getQueryString();
            HttpEntity responseHttpEntity = ExternalUtils.processRequest(blProvider.getLogics(), request.getRequestURI(),
                    query == null ? null : URLDecoder.decode(query, "utf-8"), request.getInputStream(), request.getContentType());

            if (responseHttpEntity != null) {
                response.setContentType(responseHttpEntity.getContentType().getValue());
                responseHttpEntity.writeTo(response.getOutputStream());
            } else
                response.getWriter().print("Executed successfully");

        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
            throw new RuntimeException(e);
        }
    }
}
