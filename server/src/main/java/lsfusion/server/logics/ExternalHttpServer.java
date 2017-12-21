package lsfusion.server.logics;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lsfusion.base.ExternalUtils;
import lsfusion.base.IOUtils;
import lsfusion.interop.DaemonThreadFactory;
import lsfusion.server.ServerLoggers;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.context.ThreadType;
import lsfusion.server.lifecycle.LifecycleEvent;
import lsfusion.server.lifecycle.MonitorServer;
import lsfusion.server.remote.RemoteLogics;
import org.apache.http.HttpEntity;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ExternalHttpServer extends MonitorServer {

    private static final String PARAMS_PARAM = "p";
    private static final String RETURNS_PARAM = "returns";
    private static final String SCRIPT_PARAM = "script";
    private static final String ACTION_CN_PARAM = "action";

    private LogicsInstance logicsInstance;
    private RemoteLogics remoteLogics;
    private String host;
    private int port;

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
            if (host != null)
                httpServer = HttpServer.create(new InetSocketAddress(host, port), 0);
            else
                httpServer = HttpServer.create(new InetSocketAddress(port), 0);
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

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }


    public class HttpRequestHandler implements HttpHandler {

        public void handle(HttpExchange request) {

            // поток создается HttpServer'ом, поэтому ExecutorService'ом как остальные не делается
            ThreadLocalContext.aspectBeforeMonitor(ExternalHttpServer.this, ThreadType.HTTP);
            try {
                HttpEntity response = ExternalUtils.processRequest(remoteLogics, request.getRequestURI().getPath(),
                        getParameterValue(request, ACTION_CN_PARAM), getParameterValue(request, SCRIPT_PARAM), getParameterValues(request, RETURNS_PARAM),
                        getParameterValues(request, PARAMS_PARAM), request.getRequestBody(), getContentType(request));

                if (response != null)
                    sendResponse(request, IOUtils.readBytesFromStream(response.getContent()), response.getContentType().getValue(), false);
                else
                    sendOKResponse(request);

            } catch (Exception e) {
                ServerLoggers.importLogger.error("ExternalHttpServer error: ", e);
                try {
                    sendErrorResponse(request, "Internal error occurred: " + e.getMessage());
                } catch (Exception ignored) {
                }
            } finally {
                ThreadLocalContext.aspectAfterMonitor(ThreadType.HTTP);
                request.close();
            }
        }

        private String getContentType(HttpExchange request) {
            StringBuilder result = new StringBuilder();
            List<String> contentTypeList = request.getRequestHeaders().get("Content-Type");
            if (contentTypeList != null) {
                for (String contentType : contentTypeList) {
                    result.append(contentType).append(";");
                }
            }
            return result.toString();
        }

        private String getParameterValue(HttpExchange request, String key) {
            List<String> params = getParameterValues(request, key);
            return params.isEmpty() ? null : params.get(0);
        }

        private List<String> getParameterValues(HttpExchange request, String key) {
            List<String> values = new ArrayList<>();
            String query = request.getRequestURI().getQuery();
            if (query != null) {
                for (String entry : query.split("&")) {
                    if (entry.contains("=") && entry.substring(0, entry.indexOf("=")).equals(key))
                        values.add(entry.substring(Math.min(entry.indexOf("=") + 1, entry.length() - 1)));
                }
            }
            return values;
        }

        private void sendOKResponse(HttpExchange request) throws IOException {
            sendResponse(request, "Executed successfully".getBytes(), null, false);
        }

        private void sendErrorResponse(HttpExchange request, String response) throws IOException {
            sendResponse(request, response.getBytes(), null, true);
        }

        private void sendResponse(HttpExchange request, byte[] response, String contentType, boolean error) throws IOException {
            if (contentType != null)
                request.getResponseHeaders().add("Content-Type", contentType);
            request.sendResponseHeaders(error ? 500 : 200, response.length);
            OutputStream os = request.getResponseBody();
            os.write(response);
            os.close();
        }
    }
}