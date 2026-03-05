package lsfusion.http.controller;

import lsfusion.base.ServerUtils;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.interop.connection.ComputerInfo;
import lsfusion.interop.connection.ConnectionInfo;
import lsfusion.interop.connection.UserInfo;
import lsfusion.interop.session.ExternalHttpUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

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

    public static ConnectionInfo getConnectionInfo(HttpServletRequest request) {
        Locale clientLocale = LocaleContextHolder.getLocale();

        String hostName = getRequestCookies(request).get(ServerUtils.HOSTNAME_COOKIE_NAME);
        if(hostName == null)
            hostName = request.getRemoteHost();

        Cookie timeZone = WebUtils.getCookie(request, "LSFUSION_CLIENT_TIME_ZONE");
        Cookie timeFormat = WebUtils.getCookie(request, "LSFUSION_CLIENT_TIME_FORMAT");
        Cookie dateFormat = WebUtils.getCookie(request, "LSFUSION_CLIENT_DATE_FORMAT");

        Cookie colorTheme = WebUtils.getCookie(request, "LSFUSION_CLIENT_COLOR_THEME");

        return new ConnectionInfo(new ComputerInfo(hostName, request.getRemoteAddr()), new UserInfo(clientLocale.getLanguage(), clientLocale.getCountry(), timeZone != null ? TimeZone.getTimeZone(URLDecoder.decode(timeZone.getValue())) : null, dateFormat != null ? URLDecoder.decode(dateFormat.getValue()) : null, timeFormat != null ? URLDecoder.decode(timeFormat.getValue()) : null, colorTheme != null ? colorTheme.getValue() : null));
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
