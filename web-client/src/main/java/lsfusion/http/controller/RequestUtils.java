package lsfusion.http.controller;

import lsfusion.base.BaseUtils;
import lsfusion.base.ServerUtils;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.interop.connection.ComputerInfo;
import lsfusion.interop.connection.ConnectionInfo;
import lsfusion.interop.connection.UserInfo;
import lsfusion.interop.logics.LogicsSessionObject;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.ExternalHttpUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.net.URLDecoder;
import java.util.Locale;
import java.util.TimeZone;

import static java.util.Collections.list;
import static lsfusion.gwt.client.base.GwtSharedUtils.nvl;

public class RequestUtils {

    public static RequestInfo getRequestInfo(HttpServletRequest request) {
        String query =  nvl(request.getQueryString(), "");
        String pathInfo = nvl(request.getPathInfo(), "");

        String[] headerNames = list(request.getHeaderNames()).toArray(new String[0]);
        String[] headerValues = getRequestHeaderValues(request, headerNames);

        OrderedMap<String, String> cookiesMap = getRequestCookies(request);
        String[] cookieNames = cookiesMap.keyList().toArray(new String[0]);
        String[] cookieValues = cookiesMap.values().toArray(new String[0]);
        return new RequestInfo(query, pathInfo, headerNames, headerValues, cookieNames, cookieValues);
    }

    private static String[] getRequestHeaderValues(HttpServletRequest request, String[] headerNames) {
        String[] headerValuesArray = new String[headerNames.length];
        for (int i = 0; i < headerNames.length; i++) {
            headerValuesArray[i] = StringUtils.join(list(request.getHeaders(headerNames[i])).iterator(), ",");
        }
        return headerValuesArray;
    }

    private static OrderedMap<String, String> getRequestCookies(HttpServletRequest request) {
        OrderedMap<String, String> cookiesMap = new OrderedMap<>();
        Cookie[] cookies = request.getCookies();
        if (cookies != null)
            for (Cookie cookie : cookies)
                ExternalHttpUtils.formatCookie(cookiesMap, cookie);
        return cookiesMap;
    }

    public static String getHostAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (!BaseUtils.isRedundantString(xForwardedFor)){
            String first = xForwardedFor.split(",")[0].trim();
            if (!first.isEmpty() && !"unknown".equalsIgnoreCase(first))
                return first;
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (!BaseUtils.isRedundantString(xRealIp) && !"unknown".equalsIgnoreCase(xRealIp.trim()))
            return xRealIp.trim();

        return request.getRemoteAddr();
    }

    public static ConnectionInfo getConnectionInfo(HttpServletRequest request) {
        Locale clientLocale = LocaleContextHolder.getLocale();

        String hostName = getRequestCookies(request).get(ServerUtils.HOSTNAME_COOKIE_NAME);
        if(hostName == null)
            hostName = request.getRemoteHost();

        Cookie timeZone = WebUtils.getCookie(request, "LSFUSION_CLIENT_TIME_ZONE");
        Cookie timeFormat = WebUtils.getCookie(request, "LSFUSION_CLIENT_TIME_FORMAT");
        Cookie dateFormat = WebUtils.getCookie(request, "LSFUSION_CLIENT_DATE_FORMAT");

        Cookie colorTheme = WebUtils.getCookie(request, "LSFUSION_CLIENT_COLOR_THEME");

        return new ConnectionInfo(new ComputerInfo(hostName, getHostAddress(request)), new UserInfo(clientLocale.getLanguage(), clientLocale.getCountry(), timeZone != null ? TimeZone.getTimeZone(URLDecoder.decode(timeZone.getValue())) : null, dateFormat != null ? URLDecoder.decode(dateFormat.getValue()) : null, timeFormat != null ? URLDecoder.decode(timeFormat.getValue()) : null, colorTheme != null ? colorTheme.getValue() : null));
    }

    /**
     * Read up to {@code maxBytes} from the request body, throwing
     * {@link BodyTooLargeException} on overflow rather than silently truncating. Truncation
     * would let oversize input slide into a downstream JSON parser as a malformed-tail
     * error, hiding the real "too large" problem; explicit overflow lets callers send a
     * clean 413 instead.
     *
     * <p>Used by every {@code /mcp} and {@code /oauth/*} POST handler that accepts a body
     * — the caps differ (16 MiB for MCP tool args, 64 KiB for OAuth metadata), but the
     * read-then-fail-on-overflow pattern is identical, so it lives here.
     */
    public static byte[] readBoundedBody(HttpServletRequest request, int maxBytes)
            throws java.io.IOException, BodyTooLargeException {
        java.io.InputStream is = request.getInputStream();
        if (is == null) return new byte[0];
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int total = 0;
        int n;
        while ((n = is.read(buf)) > 0) {
            total += n;
            if (total > maxBytes) throw new BodyTooLargeException();
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    public static ExternalRequest buildExternalRequest(HttpServletRequest request, byte[] body, LogicsSessionObject sessionObject) {
        RequestUtils.RequestInfo info = RequestUtils.getRequestInfo(request);

        // Same logicsHost selection as ExternalLogicsAndSessionRequestHandler — prefer the
        // configured app server host unless it is the loopback alias, in which case fall back
        // to the request's server name.
        String logicsHost = sessionObject.connection.host != null
                && !sessionObject.connection.host.equals("localhost")
                && !sessionObject.connection.host.equals("127.0.0.1")
                ? sessionObject.connection.host : request.getServerName();

        // getSession(false): MCP is wired through the security chain with `create-session=
        // never`, so most calls (tools/list, file tools, anonymous tools/call) ride without
        // an HttpSession. Calling getSession() unconditionally would conjure one for every
        // such request and defeat that contract. When no session exists we surface "" — the
        // ExternalRequest envelope expects a non-null sessionId field shape.
        HttpSession session = request.getSession(false);
        String sessionId = session != null ? session.getId() : "";

        return new ExternalRequest(
                new String[0],
                new ExternalRequest.Param[0],
                info.headerNames, info.headerValues, info.cookieNames, info.cookieValues,
                logicsHost, sessionObject.connection.port, sessionObject.connection.exportName,
                request.getScheme(), request.getMethod(), request.getServerName(), request.getServerPort(),
                nvl(request.getContextPath(), ""), nvl(request.getServletPath(), ""), info.pathInfo, info.query,
                request.getContentType(), sessionId, body,
                null, null,
                false, false);
    }

    /** Thrown by {@link #readBoundedBody} when the inbound body exceeds the cap. */
    public static final class BodyTooLargeException extends java.io.IOException {
        public BodyTooLargeException() { super(); }
    }

    public static class RequestInfo {
        public String query;
        public String pathInfo;
        public String[] headerNames;
        public String[] headerValues;
        public String[] cookieNames;
        public String[] cookieValues;

        public RequestInfo(String query, String pathInfo, String[] headerNames, String[] headerValues, String[] cookieNames, String[] cookieValues) {
            this.query = query;
            this.pathInfo = pathInfo;
            this.headerNames = headerNames;
            this.headerValues = headerValues;
            this.cookieNames = cookieNames;
            this.cookieValues = cookieValues;
        }
    }
}
