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
import lsfusion.server.base.controller.lifecycle.LifecycleEvent;
import lsfusion.server.base.controller.manager.MonitorServer;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.controller.remote.RemoteLogics;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.log.ServerLoggers;
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
import java.util.Map;
import java.util.concurrent.*;

public class ExternalHttpServer extends MonitorServer {

    private LogicsInstance logicsInstance;
    private RemoteLogics remoteLogics;
    private Map<InetSocketAddress, String> hostMap = new HashMap<>();

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
            httpServer.setExecutor(Executors.newFixedThreadPool(Settings.get().getExternalHttpServerThreadCount(), new DaemonThreadFactory("externalHttpServer-daemon")));
            httpServer.start();
        } catch (Exception e) {
            if (httpServer != null)
                httpServer.stop(0);
            e.printStackTrace();
        }
    }

    public ExternalHttpServer() {
        super(DAEMON_ORDER);
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

                ServerLoggers.httpServerLogger.info(request.getRequestURI() + "; headers: " + StringUtils.join(request.getRequestHeaders().entrySet().iterator(), ","));

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
                SessionInfo sessionInfo = new SessionInfo(getHostName(remoteAddress), address != null ? address.getHostAddress() : null, null, null);// client locale does not matter since we use anonymous authentication

                String uriPath = request.getRequestURI().getPath();
                ExecInterface remoteExec = ExternalUtils.getExecInterface(AuthenticationToken.ANONYMOUS, sessionInfo, remoteLogics);
                ExternalUtils.ExternalResponse response = ExternalUtils.processRequest(remoteExec, uriPath, request.getRequestURI().getRawQuery(),
                        request.getRequestBody(), getContentType(request), headerNames, headerValues, cookieNames, cookieValues, null,
                        null, null, null, null, null, null, null);

                if (response.response != null)
                    sendResponse(request, response);
                else
                    sendOKResponse(request);

            } catch (Exception e) {
                ServerLoggers.systemLogger.error("ExternalHttpServer error: ", e);
                try {
                    sendErrorResponse(request, e.getMessage());
                } catch (Exception ignored) {
                }
            } finally {
                ThreadLocalContext.aspectAfterMonitorHTTP(ExternalHttpServer.this);
                request.close();
            }
        }

        //we use hostMap and timeout because getHostName can be very slow
        private String getHostName(final InetSocketAddress remoteAddress) throws ExecutionException, InterruptedException {
            String hostName = hostMap.get(remoteAddress);
            if (hostName == null) {
                final Future future = Executors.newSingleThreadExecutor().submit((Callable) remoteAddress::getHostName);
                try {
                    hostName = (String) future.get(100, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    future.cancel(true);
                    hostName = "remote";
                }
                hostMap.put(remoteAddress, hostName);
            }
            return hostName;
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
            ServerLoggers.httpServerLogger.info(request.getRequestURI() + " response: " + (error ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : HttpServletResponse.SC_OK));
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
            response.getResponseHeaders().add("Access-Control-Allow-Origin","*");
            ServerLoggers.httpServerLogger.info(response.getRequestURI() + "response: " + HttpServletResponse.SC_OK);
            response.sendResponseHeaders(HttpServletResponse.SC_OK, responseEntity.getContentLength());
            responseEntity.writeTo(response.getResponseBody());
        }
    }
}