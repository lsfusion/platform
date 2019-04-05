package lsfusion.http.authentication;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LSFLoginUrlAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {

	public static RequestCache requestCache = LSFLoginUrlAuthenticationEntryPoint.createRequestCache();

	public LSFLoginUrlAuthenticationEntryPoint(String loginFormUrl) {
		super(loginFormUrl);
	}

	@Override
	protected String determineUrlToUseForThisRequest(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
		requestCache.saveRequest(request, response);
		return super.determineUrlToUseForThisRequest(request, response, exception);
	}

	private static HttpSessionRequestCache createRequestCache() {
		HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
		requestCache.setSessionAttrName("LSF_SPRING_SECURITY_SAVED_REQUEST");
		return requestCache;
	}
}