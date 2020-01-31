package lsfusion.http.authentication;

import lsfusion.base.BaseUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

//based on HttpSessionRequestCache (simplified)
public class LSFHttpSessionRequestCache {
	static final String SAVED_REQUEST = "SPRING_SECURITY_SAVED_REQUEST";
	protected final Log logger = LogFactory.getLog(this.getClass());

	private String sessionAttrName = SAVED_REQUEST;

	/**
	 * Stores the current request, provided the configuration properties allow it.
	 */
	public void saveRequest(HttpServletRequest request) {
		StringBuffer requestURL = request.getRequestURL();
		String queryString = request.getQueryString();
		if (!BaseUtils.isRedundantString(queryString)) {
			requestURL.append('?').append(queryString);
		}
		request.getSession().setAttribute(this.sessionAttrName, requestURL.toString());
		logger.debug("DefaultSavedRequest added to Session: " + requestURL);
	}

	public String getRequest(HttpServletRequest currentRequest) {
		HttpSession session = currentRequest.getSession(false);

		if (session != null) {
			return (String) session.getAttribute(this.sessionAttrName);
		}

		return null;
	}

	/**
	 * If the {@code sessionAttrName} property is set, the request is stored in
	 * the session using this attribute name. Default is
	 * "SPRING_SECURITY_SAVED_REQUEST".
	 *
	 * @param sessionAttrName a new session attribute name.
	 * @since 4.2.1
	 */
	public void setSessionAttrName(String sessionAttrName) {
		this.sessionAttrName = sessionAttrName;
	}
}