package lsfusion.gwt.base.server.spring;

import lsfusion.base.ExternalUtils;
import org.apache.http.HttpEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExternalRequestHandler implements HttpRequestHandler {
    private static final String ACTION_CN_PARAM = "action";
    private static final String SCRIPT_PARAM = "script";
    private static final String PARAMS_PARAM = "p";
    private static final String RETURNS_PARAM = "returns";

    @Autowired
    private BusinessLogicsProvider blProvider;

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            HttpEntity responseHttpEntity = ExternalUtils.processRequest(blProvider.getLogics(), request.getRequestURI(),
                    request.getParameter(ACTION_CN_PARAM), request.getParameter(SCRIPT_PARAM), getParams(request, RETURNS_PARAM),
                    getParams(request, PARAMS_PARAM), request.getInputStream(), request.getContentType());

            if (responseHttpEntity != null) {
                response.setContentType(responseHttpEntity.getContentType().getValue());
                responseHttpEntity.writeTo(response.getOutputStream());
            } else
                response.getWriter().print("Executed successfully");

        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
            throw new RuntimeException(e);
        }
    }

    private List<String> getParams(HttpServletRequest request, String key) {
        String[] params = request.getParameterValues(key);
        return params == null ? new ArrayList<String>() : Arrays.asList(params);
    }
}
