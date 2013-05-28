package platform.gwt.base.server.spring;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * c/p from org.springframework.security.web.access.AccessDeniedHandlerImpl
 */
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {
    public static final String ACCESS_DENIED_RESOURCE_URL = "ACCESS_DENIED_RESOURCE_URL";

    private String errorPage;

    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException, ServletException {
        if (!response.isCommitted()) {
            if (errorPage != null) {
                // Put exception into request scope (perhaps of use to a view)
                request.setAttribute(WebAttributes.ACCESS_DENIED_403, accessDeniedException);

                //put the original url to use on the errorPage
                StringBuffer targetUrl = request.getRequestURL();
                String queryString = request.getQueryString();
                if (queryString != null) {
                    targetUrl.append("?").append(queryString);
                }
                request.setAttribute(ACCESS_DENIED_RESOURCE_URL, targetUrl.toString());

                // Set the 403 status code.
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);

                // forward to error page.
                RequestDispatcher dispatcher = request.getRequestDispatcher(errorPage);
                dispatcher.forward(request, response);
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, accessDeniedException.getMessage());
            }
        }
    }

    public void setErrorPage(String errorPage) {
        if ((errorPage != null) && !errorPage.startsWith("/")) {
            throw new IllegalArgumentException("errorPage must begin with '/'");
        }

        this.errorPage = errorPage;
    }
}
