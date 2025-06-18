package lsfusion.server.physics.dev.integration.external.from.http;

import com.sun.net.httpserver.*;
import lsfusion.base.BaseUtils;
import lsfusion.base.DaemonThreadFactory;
import lsfusion.base.Result;
import lsfusion.base.file.FileData;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.ComputerInfo;
import lsfusion.interop.connection.ConnectionInfo;
import lsfusion.interop.connection.UserInfo;
import lsfusion.interop.connection.authentication.PasswordAuthentication;
import lsfusion.interop.session.ExecInterface;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.server.base.controller.lifecycle.LifecycleEvent;
import lsfusion.server.base.controller.manager.MonitorServer;
import lsfusion.server.base.controller.remote.RmiManager;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.controller.remote.RemoteLogics;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import org.apache.commons.lang.StringUtils;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

public class ExternalHttpServer extends MonitorServer {

    private LogicsInstance logicsInstance;
    private RemoteLogics remoteLogics;
    private final Map<InetSocketAddress, String> hostMap = new HashMap<>();

    @Override
    public String getEventName() {
        return "external-http-server";
    }

    @Override
    public LogicsInstance getLogicsInstance() {
        return logicsInstance;
    }

    private boolean useHTTPS = false;

    @Override
    protected void onStarted(LifecycleEvent event) {
        RmiManager rmiManager = getLogicsInstance().getRmiManager();
        useHTTPS = rmiManager.isHttps();
        ServerLoggers.systemLogger.info("Binding External" + (useHTTPS ? "HTTPS" : "HTTP") + "Server");
        HttpServer server = null;
        try {
            server = initServer(useHTTPS, new InetSocketAddress(rmiManager.getHttpPort()));

            server.createContext("/", new HttpRequestHandler());
            server.setExecutor(Executors.newFixedThreadPool(Settings.get().getExternalHttpServerThreadCount(), new DaemonThreadFactory("externalHttpServer-daemon")));

            server.start();
        } catch (ExternalServerException externalServerException) {
            ServerLoggers.systemLogger.error("External server has not been started. " + externalServerException.getMessage());
        } catch (Exception e) {
            if (server != null)
                server.stop(0);
            e.printStackTrace();
        }
    }

    private char[] keyPassword = null;
    private final char[] defaultKeystorePassword = getPassword(null);
    private final char[] defaultKeyPassword = getPassword(null);
    private static final String SECURITY_ALGORITHM = "SunX509";

    protected HttpServer initServer(boolean useHTTPS, InetSocketAddress inetSocketAddress) throws IOException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException, CertificateException, ExternalServerException {

        HttpServer server;
        if (useHTTPS) {
            KeyStore ks = getKeyStore();

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(SECURITY_ALGORITHM);
            kmf.init(ks, keyPassword != null ? keyPassword : defaultKeyPassword); // keyPassword = null if keystore.jks file is not used

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(SECURITY_ALGORITHM);
            tmf.init(ks);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            HttpsServer httpsServer = HttpsServer.create(inetSocketAddress, 0);
            httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
            server = httpsServer;
        } else {
            server = HttpServer.create(inetSocketAddress, 0);
        }
        return server;
    }

    static class ExternalServerException extends Exception {
        public ExternalServerException(String message) {
            super(message);
        }
    }

    private KeyStore getKeyStore() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException, ExternalServerException {
        // needed to decrypt .pem files
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
            Security.addProvider(new BouncyCastleProvider());

        ServiceLogicsModule serviceLM = getLogicsInstance().getBusinessLogics().serviceLM;
        KeyStore keystore = KeyStore.getInstance("JKS");
        try (DataSession session = createSession()) {
            Boolean useKeystore = (Boolean) serviceLM.useKeystore.read(session);
            if (useKeystore != null) {
                keyPassword = getPassword(serviceLM.keyPassword.read(session));
                char[] keystorePassword = getPassword(serviceLM.keystorePassword.read(session));

                FileData keystoreFile = (FileData) serviceLM.keystore.read(session);
                if (keystoreFile == null)
                    throw new ExternalServerException("Using external HTTPS server requires .jks file be loaded");

                keystore.load(keystoreFile.getRawFile().getInputStream(), keystorePassword);
            } else {
                // Load private key from PEM file
                FileData privateKeyFile = (FileData) serviceLM.privateKey.read(session);
                if (privateKeyFile == null)
                    throw new ExternalServerException("Using external HTTPS server requires the privateKey-file be loaded");

                PrivateKey privateKey;
                try (PEMParser pemParser = new PEMParser(new InputStreamReader(privateKeyFile.getRawFile().getInputStream(), ExternalUtils.hashCharset))) {
                    Object o = pemParser.readObject();
                    PEMKeyPair pemKeyPair;
                    if (o instanceof PEMKeyPair) {
                        pemKeyPair = (PEMKeyPair) o;
                    } else if (o instanceof PEMEncryptedKeyPair) {
                        String privateKeyPassword = (String) serviceLM.privateKeyPassword.read(session);
                        if (privateKeyPassword == null)
                            throw new ExternalServerException("PEMEncryptedKeyPair requires privateKeyPassword to be non-null");

                        pemKeyPair = ((PEMEncryptedKeyPair) o)
                                .decryptKeyPair(new JcePEMDecryptorProviderBuilder().build(privateKeyPassword.toCharArray()));
                    } else {
                        throw new ExternalServerException("Invalid PEM file");
                    }
                    privateKey = new JcaPEMKeyConverter().getKeyPair(pemKeyPair).getPrivate();
                }

                // Load certificate chain from PEM file
                FileData chainFile = (FileData) serviceLM.chain.read(session);
                if (chainFile == null)
                    throw new ExternalServerException("Using external HTTPS server requires that the certificateChain-file be loaded");

                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                X509Certificate[] chain = certFactory.generateCertificates(chainFile.getRawFile().getInputStream()).toArray(new X509Certificate[0]);

                keystore.load(null, defaultKeystorePassword);
                keystore.setKeyEntry("lsf", privateKey, defaultKeyPassword, chain);
            }
        } catch (SQLException | SQLHandledException e) {
            throw new RuntimeException(e);
        }
        return keystore;
    }

    private char[] getPassword(Object passwordObject) {
        return passwordObject != null ? ((String) passwordObject).toCharArray() : new char[0];
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

    // should be equal to ServerUtils.HOSTNAME_COOKIE_NAME
    public static final String HOSTNAME_COOKIE_NAME = "LSFUSION_HOSTNAME";
    private static final int COOKIE_VERSION = ExternalUtils.DEFAULT_COOKIE_VERSION;

    private final ExternalUtils.SessionContainer sessionContainer = new ExternalUtils.SessionContainer();

    public class HttpRequestHandler implements HttpHandler {

        public void handle(HttpExchange request) {

            // поток создается HttpServer'ом, поэтому ExecutorService'ом как остальные не делается
            ThreadLocalContext.aspectBeforeMonitorHTTP(ExternalHttpServer.this);
            try {
                Headers requestHeaders = request.getRequestHeaders();
                int headerIndex = 0;
                String[] headerNames = new String[requestHeaders.size()];
                String[] headerValues = new String[requestHeaders.size()];

                List<String> cookieNamesList = new ArrayList<>();
                List<String> cookieValuesList = new ArrayList<>();
                String hostNameCookie = null;

                String host = null;
                Integer port = null;
                for(Map.Entry<String, List<String>> requestHeader : requestHeaders.entrySet()) {
                    String headerName = requestHeader.getKey();
                    List<String> headerValue = requestHeader.getValue();

                    headerNames[headerIndex] = headerName;
                    headerValues[headerIndex++] = StringUtils.join(headerValue.iterator(), ",");

                    if(headerName.equals("Cookie")) {
                        for (String cookies : headerValue) {
                            for (String cookie : cookies.split(";")) {
                                String[] splittedCookie = cookie.split("=");
                                if (splittedCookie.length == 2) {
                                    String cookieName = splittedCookie[0];
                                    String cookieValue = ExternalUtils.decodeCookie(splittedCookie[1], 0);

                                    cookieNamesList.add(cookieName);
                                    cookieValuesList.add(cookieValue);

                                    if(cookieName.equals(HOSTNAME_COOKIE_NAME))
                                        hostNameCookie = cookieValue;
                                }
                            }
                        }
                    }
                    if(headerName.equals("Host") && !headerValue.isEmpty()) {
                        String[] hostParts = headerValue.get(0).split(":");
                        host = hostParts[0];
                        if(hostParts.length > 1)
                            port = Integer.parseInt(hostParts[1]); /*when using redirect from address without specifying a port, for example foo.bar immediately to port 7651, the port is not specified in request, and in this place when accessing host[1] the ArrayIndexOutOfBoundsException is received.*/
                    }
                }
                String[] cookieNames = cookieNamesList.toArray(new String[0]);
                String[] cookieValues = cookieValuesList.toArray(new String[0]);

                InetSocketAddress remoteAddress = request.getRemoteAddress();
                InetAddress address = remoteAddress.getAddress();

                String hostName = hostNameCookie != null ? hostNameCookie : getHostName(remoteAddress);

                ConnectionInfo connectionInfo = new ConnectionInfo(new ComputerInfo(hostName, address != null ? address.getHostAddress() : null), UserInfo.NULL);// client locale does not matter since we use anonymous authentication

                ContentType requestContentType = ExternalUtils.parseContentType(getContentType(request));

                URI requestURI = request.getRequestURI();
                String rawQuery = requestURI.getRawQuery();

                String paramSessionID = null;
                if(rawQuery != null && rawQuery.contains("session")) { // optimization
                    List<NameValuePair> queryParams = URLEncodedUtils.parse(rawQuery, ExternalUtils.defaultUrlCharset);

                    paramSessionID = ExternalUtils.getParameterValue(queryParams, "session");
                }
                Result<String> sessionID = paramSessionID != null ? new Result<>(paramSessionID) : null;
                Result<Boolean> closeSession = new Result<>(false);

                ExecInterface remoteExec = ExternalUtils.getExecInterface(remoteLogics, sessionID, closeSession, sessionContainer, getAuthToken(request), connectionInfo);

                try {
                    ExternalUtils.ExternalResponse response = ExternalUtils.processRequest(remoteExec,
                            externalRequest -> value -> getLogicsInstance().getRmiManager().convertFileValue(externalRequest, value), request.getRequestBody(), requestContentType, headerNames, headerValues, cookieNames, cookieValues, null, null, null,
                            useHTTPS ? "https" : "http", request.getRequestMethod(), host, port, "", requestURI.getPath(), "", rawQuery, null);

                    sendResponse(request, response);
                } catch (RemoteException e) {
                    closeSession.set(true); // closing session if there is a RemoteException
                    throw e;
                } finally {
                    if(sessionID != null && closeSession.result) {
                        sessionContainer.removeSession(sessionID.result);
                    }
                }
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

        private AuthenticationToken getAuthToken(HttpExchange request) throws RemoteException {
            AuthenticationToken token = AuthenticationToken.ANONYMOUS;

            List<String> authHeaders = request.getRequestHeaders().get("Authorization");
            String authHeader = authHeaders != null && !authHeaders.isEmpty() ? authHeaders.get(0) : null;

            if (authHeader != null) {
                if (authHeader.toLowerCase().startsWith("bearer ")) {
                    token = new AuthenticationToken(authHeader.substring(7));
                } else if (authHeader.toLowerCase().startsWith("basic ")) {
                    String[] credentials = BaseUtils.toHashString(Base64.getDecoder().decode(authHeader.substring(6))).split(":", 2);
                    if (credentials.length == 2)
                        token = remoteLogics.authenticateUser(new PasswordAuthentication(credentials[0], credentials[1]));
                }
            }

            return token;
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

        private String getContentType(HttpExchange request) {
            String contentType = null;
            List<String> contentTypeList = request.getRequestHeaders().get("Content-Type");
            if (contentTypeList != null && !contentTypeList.isEmpty())
                contentType = contentTypeList.get(0);
            return contentType;
        }

        private String[] getRequestHeaderValues(Headers headers, String[] headerNames) {
            String[] headerValuesArray = new String[headerNames.length];
            for (int i = 0; i < headerNames.length; i++) {
                headerValuesArray[i] = StringUtils.join(headers.get(headerNames[i]).iterator(), ",");
            }
            return headerValuesArray;
        }

        private void sendErrorResponse(HttpExchange request, String response) throws IOException {
            Charset bodyCharset = ExternalUtils.defaultBodyCharset;
            request.getResponseHeaders().add("Content-Type", "text/html; charset=" + bodyCharset.name());
            request.getResponseHeaders().add("Access-Control-Allow-Origin","*");
            byte[] responseBytes = response.getBytes(bodyCharset);
            request.sendResponseHeaders(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseBytes.length);
            OutputStream os = request.getResponseBody();
            os.write(responseBytes);
            os.close();
        }

        // copy of ExternalRequestHandler.sendResponse
        private void sendResponse(HttpExchange response, ExternalUtils.ExternalResponse responseHttpEntity) throws IOException {
            if(responseHttpEntity instanceof ExternalUtils.ResultExternalResponse) {
                sendResponse(response, (ExternalUtils.ResultExternalResponse) responseHttpEntity);
            } else
                throw new UnsupportedOperationException();
        }
        private void sendResponse(HttpExchange response, ExternalUtils.ResultExternalResponse responseHttpEntity) throws IOException {
            HttpEntity responseEntity = responseHttpEntity.response;
            String contentType = responseEntity.getContentType();
            String contentDisposition = responseHttpEntity.contentDisposition;
            String[] headerNames = responseHttpEntity.headerNames;
            String[] headerValues = responseHttpEntity.headerValues;
            String[] cookieNames = responseHttpEntity.cookieNames;
            String[] cookieValues = responseHttpEntity.cookieValues;
            int statusHttp = responseHttpEntity.statusHttp;

            boolean hasContentType = false;
            boolean hasContentDisposition = false;
            if(headerNames != null) {
                for (int i = 0; i < headerNames.length; i++) {
                    String headerName = headerNames[i];
                    if (headerName.equals("Content-Type")) {
                        hasContentType = true;
                        response.getResponseHeaders().add("Content-Type", headerValues[i]);
                    } else
                        response.getResponseHeaders().add(headerName, headerValues[i]);
                    hasContentDisposition = hasContentDisposition || headerName.equals(ExternalUtils.CONTENT_DISPOSITION_HEADER);
                }
            }

            if(cookieNames != null) {
                String cookie = "";
                for (int i = 0; i < cookieNames.length; i++) {
                    String cookieName = cookieNames[i];
                    String cookieValue = cookieValues[i];
                    cookie += (cookie.isEmpty() ? "" : ";") + cookieName + "=" + ExternalUtils.encodeCookie(cookieValue, COOKIE_VERSION);
                }
                response.getResponseHeaders().add("Cookie", cookie);
            }

            if (contentType != null && !hasContentType)
                response.getResponseHeaders().add("Content-Type", contentType);
            if(contentDisposition != null && !hasContentDisposition)
                response.getResponseHeaders().add(ExternalUtils.CONTENT_DISPOSITION_HEADER, contentDisposition);
            response.getResponseHeaders().add("Access-Control-Allow-Origin","*");
            response.sendResponseHeaders(statusHttp, responseEntity.getContentLength());
            responseEntity.writeTo(response.getResponseBody());
        }
    }
}