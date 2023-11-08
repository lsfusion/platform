package lsfusion.interop.session;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.base.file.IOUtils;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.ssl.SSLContextBuilder;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ExternalHttpUtils {

    public static ExternalHttpResponse sendRequest(ExternalHttpMethod method, String connectionString, Integer timeout, boolean insecureSSL, byte[] body,
                                                   Map<String, String> headers, Map<String, String> cookies, CookieStore cookieStore) throws IOException {

        connectionString = URIUtil.encodeQuery(connectionString);

        HttpUriRequest httpRequest;
        switch (method) {
            case GET:
                httpRequest = new HttpGet(connectionString);
                break;
            case DELETE:
                httpRequest = new HttpDeleteWithBody(connectionString);
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
            HttpEntity entity = new ByteArrayEntity(body);
            ((HttpEntityEnclosingRequestBase) httpRequest).setEntity(entity);
        }

        for (Map.Entry<String, String> headerEntry : headers.entrySet())
            httpRequest.addHeader(headerEntry.getKey(), headerEntry.getValue());
        for (Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
            cookieStore.addCookie(parseACookie(cookieEntry.getKey(), cookieEntry.getValue()));
        }

        HttpClientBuilder requestBuilder = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).useSystemProperties();

        if(timeout != null) {
            RequestConfig.Builder configBuilder = RequestConfig.custom();
            configBuilder.setSocketTimeout(timeout);
            configBuilder.setConnectTimeout(timeout);
            configBuilder.setConnectionRequestTimeout(timeout);
            requestBuilder.setDefaultRequestConfig(configBuilder.build());
        }

        if (insecureSSL) {
            try {
                requestBuilder.setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build());
                requestBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
            } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
                // do nothing
            }
        }

        HttpResponse response = (HttpResponse) BaseUtils.executeWithTimeout(() -> requestBuilder.build().execute(httpRequest), timeout);
        HttpEntity responseEntity = response.getEntity();
        ContentType responseContentType = ContentType.get(responseEntity);
        byte[] responseBytes = responseEntity != null ? IOUtils.readBytesFromHttpEntity(responseEntity) : null;
        StatusLine statusLine = response.getStatusLine();
        return new ExternalHttpResponse(responseContentType != null ? responseContentType.toString() : null, responseBytes, getResponseHeaders(response), statusLine.getStatusCode(), statusLine.getReasonPhrase());
    }

    private static Map<String, List<String>> getResponseHeaders(HttpResponse response) {
        Map<String, List<String>> responseHeaders = new HashMap<>();
        for(Header header : response.getAllHeaders()) {
            String headerName = header.getName();
            List<String> headerValues = responseHeaders.computeIfAbsent(headerName, k -> new ArrayList<>());
            headerValues.add(header.getValue());
        }
        return responseHeaders;
    }

    public static void formatCookie(OrderedMap<String, String> result, Cookie cookie) {
        result.put(cookie.getName(), ExternalUtils.decodeCookie(cookie.getValue(), cookie.getVersion()));
    }
    public static void formatCookie(OrderedMap<String, String> result, org.apache.http.cookie.Cookie cookie) {
        result.put(cookie.getName(), ExternalUtils.decodeCookie(cookie.getValue(), cookie.getVersion()));
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
    public static org.apache.http.cookie.Cookie parseACookie(String cookieName, String rawCookie) {
        String[] rawCookieParams = rawCookie.split(";");

        int version = ExternalUtils.DEFAULT_COOKIE_VERSION;
        String cookieValue = ExternalUtils.encodeCookie(rawCookieParams[0], version);

        BasicClientCookie cookie = new BasicClientCookie(cookieName, cookieValue);
        cookie.setVersion(version);

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