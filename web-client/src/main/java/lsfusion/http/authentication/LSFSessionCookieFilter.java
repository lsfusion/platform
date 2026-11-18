package lsfusion.http.authentication;

import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static lsfusion.base.BaseUtils.firstWord;

// The web client's cookies of the browser's session. A response sets the site cookie (LSFCrossSiteRequestFilter)
// unless its request has it. The session's cookie is SameSite=Lax (see web.xml), and Tomcat sets a SameSite only by a
// CookieProcessor of the context file, which an installed web client has apart from the war. So the session's
// Set-Cookie, which the container adds when a request creates the session or changes its id, gets one here unless it
// has one. The filter stands first, so that every filter and servlet after it gets the session through it.
public class LSFSessionCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        LSFCrossSiteRequestFilter.setSiteCookie(request, response);
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override
            public HttpSession getSession() {
                return getSession(true);
            }

            @Override
            public HttpSession getSession(boolean create) {
                HttpSession session = super.getSession(create);
                if (create)
                    setSameSite(response, session.getId());
                return session;
            }

            @Override
            public String changeSessionId() {
                String id = super.changeSessionId();
                setSameSite(response, id);
                return id;
            }
        }, response);
    }

    // the session's Set-Cookie is found by its value, the session's id (Jetty's goes on with a dot and the node), as
    // its name is the container's
    private static void setSameSite(HttpServletResponse response, String id) {
        List<String> cookies = new ArrayList<>(response.getHeaders("Set-Cookie"));
        for (int i = 0; i < cookies.size(); i++) {
            String cookie = cookies.get(i), pair = firstWord(cookie, ";"), value = pair.substring(pair.indexOf('=') + 1);
            if ((value.equals(id) || value.startsWith(id + ".")) && !hasSameSite(cookie)) {
                cookies.set(i, cookie + "; SameSite=Lax");
                // a response changes no single header of several with a name: they are all set anew
                response.setHeader("Set-Cookie", cookies.get(0));
                for (String other : cookies.subList(1, cookies.size()))
                    response.addHeader("Set-Cookie", other);
                return;
            }
        }
    }

    // an attribute named SameSite after the name and the value, not the word in another one (a Path=/samesite)
    private static boolean hasSameSite(String cookie) {
        return Arrays.stream(cookie.split(";")).skip(1).anyMatch(attribute -> firstWord(attribute, "=").trim().equalsIgnoreCase("SameSite"));
    }
}
