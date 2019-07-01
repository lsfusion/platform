package lsfusion.http.authentication;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LSFLoginUrlAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {

	public static LSFHttpSessionRequestCache requestCache = LSFLoginUrlAuthenticationEntryPoint.createRequestCache();

	public LSFLoginUrlAuthenticationEntryPoint(String loginFormUrl) {
		super(loginFormUrl);
	}

	@Override
	protected String determineUrlToUseForThisRequest(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
		requestCache.saveRequest(request);
		return super.determineUrlToUseForThisRequest(request, response, exception);
	}

	private static LSFHttpSessionRequestCache createRequestCache() {
		LSFHttpSessionRequestCache requestCache = new LSFHttpSessionRequestCache();
		requestCache.setSessionAttrName("LSF_SPRING_SECURITY_SAVED_REQUEST");
		return requestCache;
	}
}