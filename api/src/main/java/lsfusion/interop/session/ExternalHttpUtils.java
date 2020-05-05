package lsfusion.interop.session;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.file.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static lsfusion.interop.session.ExternalHttpMethod.PUT;

public class ExternalHttpUtils {

    public static ExternalHttpResponse readHTTP(ExternalHttpMethod method, String connectionString, String bodyUrl, Object[] paramList, ImMap<String, String> headers, ImMap<String, String> cookies, CookieStore cookieStore) throws IOException {
        HttpUriRequest httpRequest;
        switch (method) {
            case GET: {
                httpRequest = new HttpGet(connectionString);
                break;
            }
            case DELETE: {
                httpRequest = new HttpDelete(connectionString);
                break;
            }
            case PUT:
            case POST:
            default: {
                if(method.equals(PUT))
                    httpRequest = new HttpPut(connectionString);
                else
                    httpRequest = new HttpPost(connectionString);
                HttpEntity entity = ExternalUtils.getInputStreamFromList(paramList, bodyUrl, null);
                if (!headers.containsKey("Content-Type"))
                    httpRequest.addHeader("Content-Type", entity.getContentType().getValue());
                ((HttpEntityEnclosingRequestBase) httpRequest).setEntity(entity);
                break;
            }
        }
        for(int i=0,size=headers.size();i<size;i++)
            httpRequest.addHeader(headers.getKey(i), headers.getValue(i));
        for(int i=0,size=cookies.size();i<size;i++) {
            BasicClientCookie cookie = parseRawCookie(cookies.getKey(i), cookies.getValue(i));
            cookieStore.addCookie(cookie);
        }

        HttpResponse response = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).useSystemProperties().build().execute(httpRequest);
        HttpEntity responseEntity = response.getEntity();
        ContentType contentType = ContentType.get(responseEntity);
        byte[] responseBytes = IOUtils.readBytesFromStream(responseEntity.getContent());
        StatusLine statusLine = response.getStatusLine();
        return new ExternalHttpResponse(contentType != null ? contentType.toString() : null, responseBytes, getResponseHeaders(response), statusLine.getStatusCode(), statusLine.getReasonPhrase());
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

    private static BasicClientCookie parseRawCookie(String cookieName, String rawCookie) {
        BasicClientCookie cookie;
        String[] rawCookieParams = rawCookie.split(";");

        String cookieValue = rawCookieParams[0];

        cookie = new BasicClientCookie(cookieName, cookieValue);

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