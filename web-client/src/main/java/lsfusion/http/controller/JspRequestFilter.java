package lsfusion.http.controller;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

// a JSP is only a view the web client forwards to (web.xml maps this filter to requests, not to forwards), so a request the
// container would hand to its JSP servlet - by the extensions Tomcat and Jetty map to it, which make the whole path the
// servlet path - is answered with 404. A downloaded file named so
// passes, its name is in the path info of /file/*
public class JspRequestFilter implements Filter {

    private static final String[] JSP_EXTENSIONS = {".jsp", ".jspx", ".jspf", ".xsp"};

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String servletPath = ((HttpServletRequest) request).getServletPath().toLowerCase();
        for(String extension : JSP_EXTENSIONS)
            if(servletPath.endsWith(extension)) {
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
