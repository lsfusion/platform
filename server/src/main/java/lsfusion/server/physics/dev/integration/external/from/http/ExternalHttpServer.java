package lsfusion.server.physics.dev.integration.external.from.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lsfusion.base.DaemonThreadFactory;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.session.ExecInterface;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.interop.session.SessionInfo;
import lsfusion.server.ServerLoggers;
import lsfusion.server.base.context.ThreadLocalContext;
import lsfusion.server.base.lifecycle.LifecycleEvent;
import lsfusion.server.base.lifecycle.MonitorServer;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.remote.RemoteLogics;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;

public class ExternalHttpServer extends MonitorServer {

    private LogicsInstance logicsInstance;
    private RemoteLogics remoteLogics;

    @Override
    public String getEventName() {
        return "external-http-server";
    }

    @Override
    public LogicsInstance getLogicsInstance() {
        return logicsInstance;
    }

    @Override
    protected void onInit(LifecycleEvent event) {
    }

    @Override
    protected void onStarted(LifecycleEvent event) {
        ServerLoggers.systemLogger.info("Binding ExternalHttpServer");
        HttpServer httpServer = null;
        try {
            httpServer = HttpServer.create(new InetSocketAddress(getLogicsInstance().getRmiManager().getHttpPort()), 0);
            httpServer.createContext("/", new HttpRequestHandler());
            httpServer.setExecutor(Executors.newFixedThreadPool(10, new DaemonThreadFactory("externalHttpServer-daemon")));
            httpServer.start();
        } catch (Exception e) {
            if (httpServer != null)
                httpServer.stop(0);
            e.printStackTrace();
        }
    }

    public ExternalHttpServer() {
        super(HIGH_DAEMON_ORDER);
    }

    public void setLogicsInstance(LogicsInstance logicsInstance) {
        this.logicsInstance = logicsInstance;
    }

    public void setRemoteLogics(RemoteLogics remoteLogics) {
        this.remoteLogics = remoteLogics;
    }

    public class HttpRequestHandler implements HttpHandler {

        public void handle(HttpExchange request) {

            // поток создается HttpServer'ом, поэтому ExecutorService'ом как остальные не делается
            ThreadLocalContext.aspectBeforeMonitorHTTP(ExternalHttpServer.this);
            try {
                String[] headerNames = request.getRequestHeaders().keySet().toArray(new String[0]);
                String[] headerValues = getRequestHeaderValues(request.getRequestHeaders(), headerNames);

                OrderedMap<String, String> cookiesMap = new OrderedMap<>();
                List<String> cookiesList = request.getRequestHeaders().get("Cookie");
                if(cookiesList != null) {
                    for (String cookies : cookiesList) {
                        for (String cookie : cookies.split(";")) {
                            String[] splittedCookie = cookie.split("=");
                            if (splittedCookie.length == 2) {
                                cookiesMap.put(splittedCookie[0], splittedCookie[1]);
                            }
                        }
                    }
                }

                String[] cookieNames = cookiesMap.keyList().toArray(new String[0]);
                String[] cookieValues = cookiesMap.values().toArray(new String[0]);

                InetSocketAddress remoteAddress = request.getRemoteAddress();
                InetAddress address = remoteAddress.getAddress();
                SessionInfo sessionInfo = new SessionInfo(remoteAddress.getHostName(), address != null ? address.getHostAddress() : null, null, null);// client locale does not matter since we use anonymous authentication

                String uriPath = request.getRequestURI().getPath();
                String url = "http://" + request.getRequestHeaders().getFirst("Host") + uriPath;
                ExecInterface remoteExec = ExternalUtils.getExecInterface(AuthenticationToken.ANONYMOUS, sessionInfo, remoteLogics);
                ExternalUtils.ExternalResponse response = ExternalUtils.processRequest(remoteExec, url, uriPath, request.getRequestURI().getRawQuery(), 
                        request.getRequestBody(), new HashMap<String, String[]>(), getContentType(request), headerNames, headerValues, cookieNames, cookieValues, null, null, null);

                if (response.response != null)
                    sendResponse(request, response);
                else
                    sendOKResponse(request);

            } catch (Exception e) {
                ServerLoggers.importLogger.error("ExternalHttpServer error: ", e);
                try {
                    sendErrorResponse(request, e.getMessage());
                } catch (Exception ignored) {
                }
            } finally {
                ThreadLocalContext.aspectAfterMonitorHTTP(ExternalHttpServer.this);
                request.close();
            }
        }

        private ContentType getContentType(HttpExchange request) {
            List<String> contentTypeList = request.getRequestHeaders().get("Content-Type");
            if (contentTypeList != null) {
                for (String contentType : contentTypeList) {
                    return ContentType.parse(contentType);
                }
            }
            return null;
        }
        
        private String[] getRequestHeaderValues(Headers headers, String[] headerNames) {
            String[] headerValuesArray = new String[headerNames.length];
            for (int i = 0; i < headerNames.length; i++) {
                headerValuesArray[i] = StringUtils.join(headers.get(headerNames[i]).iterator(), ",");
            }
            return headerValuesArray;
        }

        private void sendOKResponse(HttpExchange request) throws IOException {
            sendResponse(request, "Executed successfully".getBytes(), false);
        }

        private void sendErrorResponse(HttpExchange request, String response) throws IOException {
            sendResponse(request, response.getBytes(), true);
        }

        private void sendResponse(HttpExchange request, byte[] response, boolean error) throws IOException {
            request.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            request.sendResponseHeaders(error ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : HttpServletResponse.SC_OK, response.length);
            OutputStream os = request.getResponseBody();
            os.write(response);
            os.close();
        }

        // copy of ExternalRequestHandler.sendResponse
        private void sendResponse(HttpExchange response, ExternalUtils.ExternalResponse responseHttpEntity) throws IOException {
            HttpEntity responseEntity = responseHttpEntity.response;
            String contentType = responseEntity.getContentType().getValue();
            String contentDisposition = responseHttpEntity.contentDisposition;
            String[] headerNames = responseHttpEntity.headerNames;
            String[] headerValues = responseHttpEntity.headerValues;
            String[] cookieNames = responseHttpEntity.cookieNames;
            String[] cookieValues = responseHttpEntity.cookieValues;

            boolean hasContentType = false;
            boolean hasContentDisposition = false;
            for(int i=0;i<headerNames.length;i++) {
                String headerName = headerNames[i];
                if(headerName.equals("Content-Type")) {
                    hasContentType = true;
                    response.getResponseHeaders().add("Content-Type", headerValues[i]);
                } else
                    response.getResponseHeaders().add(headerName, headerValues[i]);
                hasContentDisposition = hasContentDisposition || headerName.equals("Content-Disposition");
            }

            String cookie = "";
            for(int i=0;i<cookieNames.length;i++) {
                String cookieName = cookieNames[i];
                String cookieValue = cookieValues[i];
                cookie += (cookie.isEmpty() ? "" : ";") + cookieName + "=" + cookieValue;
            }
            response.getResponseHeaders().add("Cookie", cookie);

            if (contentType != null && !hasContentType)
                response.getResponseHeaders().add("Content-Type", contentType);
            if(contentDisposition != null && !hasContentDisposition)
                response.getResponseHeaders().add("Content-Disposition", contentDisposition);
            response.sendResponseHeaders(HttpServletResponse.SC_OK, responseEntity.getContentLength());
            responseEntity.writeTo(response.getResponseBody());
        }
    }
}