package lsfusion.interop.session;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.base.file.IOUtils;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.ssl.SSLContextBuilder;

import javax.net.ssl.SSLContext;
import javax.servlet.http.Cookie;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ExternalHttpUtils {

    public static ExternalHttpResponse sendRequest(ExternalHttpMethod method, String connectionString, Long timeout, boolean insecureSSL, byte[] body,
                                                   Map<String, String> headers, Map<String, String> cookies, CookieStore cookieStore) throws IOException {

//        connectionString = URIUtil.encodeQuery(connectionString);

        HttpUriRequest httpRequest;
        switch (method) {
            case GET:
                httpRequest = new HttpGet(connectionString);
                break;
            case DELETE:
                httpRequest = new HttpDelete(connectionString);
                break;
            case PATCH:
                httpRequest = new HttpPatch(connectionString);
                break;
            case PUT:
                httpRequest = new HttpPut(connectionString);
                break;
            case POST:
            default:
                httpRequest = new HttpPost(connectionString);
                break;
        }
        if(body != null) {
            httpRequest.setEntity(new ByteArrayEntity(body, null));
        }

        for (Map.Entry<String, String> headerEntry : headers.entrySet())
            httpRequest.addHeader(headerEntry.getKey(), headerEntry.getValue());
        for (Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
            cookieStore.addCookie(parseACookie(cookieEntry.getKey(), cookieEntry.getValue()));
        }

        HttpClientBuilder requestBuilder = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).useSystemProperties();

        if(timeout != null) {
            RequestConfig.Builder configBuilder = RequestConfig.custom();
            configBuilder.setConnectTimeout(timeout, TimeUnit.MILLISECONDS);
            configBuilder.setConnectionRequestTimeout(timeout, TimeUnit.MILLISECONDS);
            configBuilder.setResponseTimeout(timeout, TimeUnit.MILLISECONDS);
            requestBuilder.setDefaultRequestConfig(configBuilder.build());
        }

        if (insecureSSL) {
            try {
                SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build();
                Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                                .register("https", new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE))
                                .register("http", new PlainConnectionSocketFactory())
                                .build();
                requestBuilder.setConnectionManager(new BasicHttpClientConnectionManager(registry));
            } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
                // do nothing
            }
        }

        try (CloseableHttpClient httpClient = requestBuilder.build()) {
            CloseableHttpResponse response = (CloseableHttpResponse) BaseUtils.executeWithTimeout(() -> httpClient.execute(httpRequest), timeout);
            HttpEntity responseEntity = response.getEntity();
            String responseContentType = responseEntity != null ? responseEntity.getContentType() : null;
            byte[] responseBytes = responseEntity != null ? IOUtils.readBytesFromHttpEntity(responseEntity) : null;
            return new ExternalHttpResponse(responseContentType, responseBytes, getResponseHeaders(response), response.getCode(), response.getReasonPhrase());
        }
    }

    private static Map<String, List<String>> getResponseHeaders(HttpResponse response) {
        Map<String, List<String>> responseHeaders = new HashMap<>();
        for(Header header : response.getHeaders()) {
            String headerName = header.getName();
            List<String> headerValues = responseHeaders.computeIfAbsent(headerName, k -> new ArrayList<>());
            headerValues.add(header.getValue());
        }
        return responseHeaders;
    }

    public static void formatCookie(OrderedMap<String, String> result, Cookie cookie) {
        result.put(cookie.getName(), ExternalUtils.decodeCookie(cookie.getValue(), cookie.getVersion()));
    }
    public static void formatCookie(OrderedMap<String, String> result, org.apache.hc.client5.http.cookie.Cookie cookie) {
        result.put(cookie.getName(), ExternalUtils.decodeCookie(cookie.getValue(), ExternalUtils.DEFAULT_COOKIE_VERSION));
    }
    public static Cookie parseCookie(String cookieName, String rawCookie) {
//        int version = ExternalUtils.DEFAULT_COOKIE_VERSION;
//
//        Cookie cookie = new Cookie(cookieName, ExternalUtils.encodeCookie(rawCookie, version));
//        cookie.setVersion(version);
//
//        return cookie;

        String[] rawCookieParams = rawCookie.split(";");

        int version = ExternalUtils.DEFAULT_COOKIE_VERSION;
        String cookieValue = ExternalUtils.encodeCookie(rawCookieParams[0], version);

        Cookie cookie = new Cookie(cookieName, cookieValue);
        cookie.setVersion(version);

        for (int i = 1; i < rawCookieParams.length; i++) {

            String[] rawCookieParam = rawCookieParams[i].split("=");
            String paramName = rawCookieParam[0].trim();

            if (paramName.equalsIgnoreCase("secure")) {
                cookie.setSecure(true);
            } else if (rawCookieParam.length == 2) {
                String paramValue = rawCookieParam[1].trim();

                if (paramName.equalsIgnoreCase("expires")) {
                    cookie.setMaxAge((int) (parseDate(paramValue).getTime() - System.currentTimeMillis()));
                } else if (paramName.equalsIgnoreCase("max-age")) {
                    cookie.setMaxAge(Integer.parseInt(paramValue));
                } else if (paramName.equalsIgnoreCase("domain")) {
                    cookie.setDomain(paramValue);
                } else if (paramName.equalsIgnoreCase("path")) {
                    cookie.setPath(paramValue);
                } else if (paramName.equalsIgnoreCase("comment")) {
                    cookie.setPath(paramValue);
                }
            }
        }
        return cookie;
    }
    public static BasicClientCookie parseACookie(String cookieName, String rawCookie) {
        String[] rawCookieParams = rawCookie.split(";");

        String cookieValue = ExternalUtils.encodeCookie(rawCookieParams[0], ExternalUtils.DEFAULT_COOKIE_VERSION);

        BasicClientCookie cookie = new BasicClientCookie(cookieName, cookieValue);

        for (int i = 1; i < rawCookieParams.length; i++) {

            String[] rawCookieParam = rawCookieParams[i].split("=");
            String paramName = rawCookieParam[0].trim();

            if (paramName.equalsIgnoreCase("secure")) {
                cookie.setSecure(true);
            } else if (rawCookieParam.length == 2) {
                String paramValue = rawCookieParam[1].trim();

                if (paramName.equalsIgnoreCase("expires")) {
                    cookie.setExpiryDate(parseDate(paramValue));
                } else if (paramName.equalsIgnoreCase("max-age")) {
                    long maxAge = Long.parseLong(paramValue);
                    Date expiryDate = new Date(System.currentTimeMillis() + maxAge);
                    cookie.setExpiryDate(expiryDate);
                } else if (paramName.equalsIgnoreCase("domain")) {
                    cookie.setDomain(paramValue);
                } else if (paramName.equalsIgnoreCase("path")) {
                    cookie.setPath(paramValue);
                } else if (paramName.equalsIgnoreCase("comment")) {
                    cookie.setPath(paramValue);
                }
            }
        }
        return cookie;
    }

    private static Date parseDate(String value) {
        try {
            return new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ssZZZ").parse(value);
        } catch (java.text.ParseException e) {
            return null;
        }
    }
}