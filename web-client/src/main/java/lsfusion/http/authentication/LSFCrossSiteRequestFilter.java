package lsfusion.http.authentication;

import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;

// The API (/exec, /eval, /form, /files, /mcp) runs a request on a logged-in session of the web client as the session's
// user only when the browser says it comes from a page of the web client: by Sec-Fetch-Site, which it sends over https
// and to localhost, else by the Origin it sends with a request other than a GET or HEAD, else by the Referer. A request
// the user opens - typed, a bookmark, a link another application hands to the browser - comes from no page, so it does
// not run on the session. A request other than a GET or HEAD with none of them runs: a browser sends an Origin with it.
// The filter stands right after the one that reads the session, so the authentication it sees is the session's.
public class LSFCrossSiteRequestFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        if (!LSFAuthenticationToken.getAppServerToken().isAnonymous() && !isAllowed(request)) {
            if (!hasBasicLogin(request)) {
                refuse(response);
                return;
            }
            // the request runs as its own login, which the Basic filter then checks
            SecurityContextHolder.clearContext();
        }
        chain.doFilter(request, response);
    }

    // an Authorization header the Basic filter reads (BasicAuthenticationConverter)
    private static boolean hasBasicLogin(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && StringUtils.startsWithIgnoreCase(header.trim(), BasicAuthenticationConverter.AUTHENTICATION_SCHEME_BASIC);
    }

    // the answer to a request that does not run on the session
    private static void refuse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().print("The web client cannot use the logged-in session for this request.");
    }

    // a request that may run on the session; the login opens no other one after it either, on the session it logs in
    // (ExternalRequestHandler.processFailedAuthentication)
    public static boolean isAllowed(HttpServletRequest request) {
        String site = request.getHeader("Sec-Fetch-Site");
        if (site != null)
            return site.equals("same-origin");
        String origin = request.getHeader("Origin");
        if (origin != null)
            return isOwn(origin, request);
        String referer = request.getHeader("Referer");
        if (referer != null)
            return isOwn(referer, request);
        return !isGet(request);
    }

    // an Origin or a Referer of the web client, by host and port alone: behind a proxy that ends TLS the request's
    // scheme is not the page's
    private static boolean isOwn(String url, HttpServletRequest request) {
        try {
            String authority = URI.create(url).getRawAuthority(); // null for "null"
            return authority != null && authority.equalsIgnoreCase(request.getHeader("Host"));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean isGet(HttpServletRequest request) {
        return request.getMethod().equals("GET") || request.getMethod().equals("HEAD");
    }
}
