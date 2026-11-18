package lsfusion.http.authentication;

import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;

// The API (/exec, /eval, /form, /files, /mcp) runs a request on a logged-in session of the web client as the session's
// user only when the browser says it comes from a page of the web client or from the user (the address bar, a
// bookmark). A GET or HEAD with neither Sec-Fetch-Site nor Origin - a client other than a browser sends neither, nor
// does a browser over plain http to a host other than localhost - runs on the session only with the site cookie.
// The filter stands right after the one that reads the session, so the authentication it sees is the session's.
public class LSFCrossSiteRequestFilter extends OncePerRequestFilter {

    // a cookie a response sets unless its request has it (LSFSessionCookieFilter), SameSite=Strict: a browser sends it
    // with a request of the site's own pages and with one the user opens - typed, a bookmark -, not with another site's
    private static final String SITE_COOKIE = "LSFUSION_SAME_SITE";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        if (ridesOnSession(request) && !isAllowed(request)) {
            if (!hasBasicLogin(request)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().print("The web client cannot use the logged-in session for this request.");
                return;
            }
            // the request runs as its own login, which the Basic filter then checks
            SecurityContextHolder.clearContext();
        }
        chain.doFilter(request, response);
    }

    // a request with a Bearer token runs as the token's user (LSFAuthTokenFilter)
    private static boolean ridesOnSession(HttpServletRequest request) {
        return !LSFAuthenticationToken.getAppServerToken().isAnonymous() && LSFAuthTokenFilter.getBearerToken(request) == null;
    }

    // an Authorization header the Basic filter reads (BasicAuthenticationConverter)
    private static boolean hasBasicLogin(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && StringUtils.startsWithIgnoreCase(header.trim(), BasicAuthenticationConverter.AUTHENTICATION_SCHEME_BASIC);
    }

    // a request that may run on the session; the login opens no other one after it either, on the session it logs in
    // (ExternalRequestHandler.processFailedAuthentication)
    public static boolean isAllowed(HttpServletRequest request) {
        String site = getSite(request);
        return site == null ? !isGet(request) || hasSiteCookie(request) : site.equals("same-origin") || site.equals("none");
    }

    // the site cookie, for every path of the web client, unless the request has it
    public static void setSiteCookie(HttpServletRequest request, HttpServletResponse response) {
        if (!hasSiteCookie(request))
            response.addHeader("Set-Cookie", SITE_COOKIE + "=1; Path=" + request.getServletContext().getContextPath() + "/; HttpOnly; SameSite=Strict");
    }

    private static boolean hasSiteCookie(HttpServletRequest request) {
        return WebUtils.getCookie(request, SITE_COOKIE) != null;
    }

    // Sec-Fetch-Site, which a browser sends to https and localhost; elsewhere the Origin it sends with a POST
    private static String getSite(HttpServletRequest request) {
        String site = request.getHeader("Sec-Fetch-Site");
        if (site != null)
            return site;
        String origin = request.getHeader("Origin");
        if (origin == null)
            return null;
        return isOrigin(origin, request.getHeader("Host")) ? "same-origin" : "cross-site";
    }

    // by host and port alone: behind a proxy that ends TLS the request's scheme is not the page's
    private static boolean isOrigin(String origin, String host) {
        try {
            String authority = URI.create(origin).getRawAuthority(); // null for "null"
            return authority != null && authority.equalsIgnoreCase(host);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean isGet(HttpServletRequest request) {
        return request.getMethod().equals("GET") || request.getMethod().equals("HEAD");
    }
}
