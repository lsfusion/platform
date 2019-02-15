package lsfusion.http;

import lsfusion.base.ExternalUtils;
import lsfusion.base.SessionInfo;
import lsfusion.gwt.server.logics.LogicsConnection;
import lsfusion.gwt.server.navigator.provider.LogicsAndNavigatorProviderImpl;
import lsfusion.interop.RemoteLogicsInterface;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

import static java.util.Collections.list;
import static lsfusion.base.ServerMessages.getString;

public class ExternalRequestHandler extends HttpLogicsRequestHandler {

    @Override
    protected void handleRequest(RemoteLogicsInterface remoteLogics, LogicsConnection logicsConnection, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String query = request.getQueryString();
            String contentTypeString = request.getContentType();
            ContentType contentType = contentTypeString != null ? ContentType.parse(contentTypeString) : null;

            String[] headerNames = list((Enumeration<String>)request.getHeaderNames()).toArray(new String[0]);
            String[] headerValues = getRequestHeaderValues(request, headerNames);
            
            ExternalUtils.ExternalResponse responseHttpEntity = ExternalUtils.processRequest(LSFAuthenticationToken.getAppServerToken(), 
                    LogicsAndNavigatorProviderImpl.getSessionInfo(request), remoteLogics, request.getRequestURI(), query == null ? "" : query, request.getInputStream(), contentType, headerNames, headerValues);

            if (responseHttpEntity.response != null) {
                sendResponse(response, responseHttpEntity);
            } else {
                sendOKResponse(request, response);
            }

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("text/html; charset=utf-8");
            response.getWriter().print(getString(request, "internal.server.error.with.message", e.getMessage()));
        }
    }

    private void sendOKResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.getWriter().print(getString(request, "executed.successfully"));
    }

    // copy of ExternalHTTPServer.sendResponse
    private void sendResponse(HttpServletResponse response, ExternalUtils.ExternalResponse responseHttpEntity) throws IOException {
        HttpEntity responseEntity = responseHttpEntity.response;
        Header contentType = responseEntity.getContentType();
        String contentDisposition = responseHttpEntity.contentDisposition;
        String[] headerNames = responseHttpEntity.headerNames;
        String[] headerValues = responseHttpEntity.headerNames;

        boolean hasContentType = false; 
        boolean hasContentDisposition = false; 
        for(int i=0;i<headerNames.length;i++) {
            String headerName = headerNames[i];
            if(headerName.equals("Content-Type")) {
                hasContentType = true;
                response.setContentType(headerValues[i]);
            } else                
                response.addHeader(headerName, headerValues[i]);
            hasContentDisposition = hasContentDisposition || headerName.equals("Content-Disposition");            
        }
        if(contentType != null && !hasContentType)
            response.setContentType(contentType.getValue());
        if(contentDisposition != null && !hasContentDisposition)
            response.addHeader("Content-Disposition", contentDisposition);        
        response.setStatus(HttpServletResponse.SC_OK);
        responseEntity.writeTo(response.getOutputStream());
    }

    private String[] getRequestHeaderValues(HttpServletRequest request, String[] headerNames) {
        String[] headerValuesArray = new String[headerNames.length];
        for (int i = 0; i < headerNames.length; i++) {
            headerValuesArray[i] = StringUtils.join(list(request.getHeaders(headerNames[i])), ",");
        }
        return headerValuesArray;
    }
}
