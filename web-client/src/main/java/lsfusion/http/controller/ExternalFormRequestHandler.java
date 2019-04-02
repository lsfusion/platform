package lsfusion.http.controller;

import com.google.common.base.Throwables;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.Pair;
import lsfusion.base.file.IOUtils;
import lsfusion.http.provider.form.FormProvider;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.http.provider.navigator.NavigatorProvider;
import lsfusion.http.provider.navigator.NavigatorSessionObject;
import lsfusion.http.provider.session.SessionProvider;
import lsfusion.interop.base.exception.AuthenticationException;
import lsfusion.interop.base.exception.RemoteInternalException;
import lsfusion.interop.form.remote.RemoteFormInterface;
import lsfusion.interop.logics.LogicsSessionObject;
import lsfusion.interop.session.ExternalUtils;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.rmi.RemoteException;

import static lsfusion.gwt.server.form.FormActionHandler.defaultLastReceivedRequestIndex;

public class ExternalFormRequestHandler extends ExternalRequestHandler {

    @Autowired
    NavigatorProvider logicsAndNavigatorProvider;

    @Autowired
    SessionProvider sessionProvider;

    @Autowired
    FormProvider formProvider;

    @Override
    protected void handleRequest(LogicsSessionObject sessionObject, HttpServletRequest request, HttpServletResponse response) throws RemoteException {
        String sessionID = null;
        boolean closeSession = false;
        try {
            String contentTypeString = request.getContentType();
            ContentType contentType = contentTypeString != null ? ContentType.parse(contentTypeString) : null;

            Charset charset = ExternalUtils.getCharsetFromContentType(contentType);
            String json = new String(IOUtils.readBytesFromStream(request.getInputStream()), charset);
            JSONObject jsonObject = new JSONObject(json);

            sessionID = getSessionID(jsonObject);
            String action = jsonObject.optString("action");

            switch (action) {
                case "create": {
                    sessionID = logicsAndNavigatorProvider.createOrGetNavigatorSessionObject(sessionObject, request, sessionID);
                    NavigatorSessionObject navigatorSessionObject = logicsAndNavigatorProvider.getNavigatorSessionObject(sessionID);

                    Pair<RemoteFormInterface, String> result = navigatorSessionObject.remoteNavigator.createFormExternal(json);
                    sessionID = formProvider.createFormExternal(result.first, sessionID);
                    JSONObject jsonResponse = new JSONObject(result.second);
                    jsonResponse.put("sessionID", sessionID);
                    sendResponse(request, response, jsonResponse.toString(), charset, false, true);
                    break;
                }
                case "change": {
                    FormSessionObject formSessionObject = formProvider.getFormSessionObject(sessionID);
                    if (formSessionObject != null) {
                        Pair<Long, String> result = formSessionObject.remoteForm.changeExternal(formSessionObject.requestIndex++, defaultLastReceivedRequestIndex, json);
                        sendResponse(request, response, result.second, charset, false, true);
                    } else {
                        sendErrorResponse(request, response, "Invalid change request");
                    }
                    break;
                }
                case "close": {
                    FormSessionObject formSessionObject = formProvider.getFormSessionObject(sessionID);
                    if (formSessionObject != null) {
                        formSessionObject.remoteForm.closeExternal();
                        formProvider.removeFormSessionObject(sessionID);
                        JSONObject responseObject = new JSONObject();
                        responseObject.put("message", "session closed");
                        sendResponse(request, response, responseObject.toString(), charset, false, true);
                    } else {
                        sendErrorResponse(request, response, "Invalid close request");
                    }
                    break;
                }
                default:
                    sendErrorResponse(request, response, "Unknown action: '" + action + "'");
            }


        } catch (Exception e) {
            if (e instanceof AuthenticationException) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("text/html; charset=utf-8");
                try { // in theory here can be changed exception (despite the fact that remote call is wrapped into RemoteExceptionAspect)
                    Pair<String, Pair<String, String>> actualStacks = RemoteInternalException.toString(e);
                    response.getWriter().print(actualStacks.first + '\n' + ExceptionUtils.getExStackTrace(actualStacks.second.first, actualStacks.second.second));
                } catch (IOException e1) {
                    throw Throwables.propagate(e1);
                }

                if (e instanceof RemoteException) { // rethrow RemoteException to invalidate LogicsSessionObject in LogicsProvider
                    closeSession = true; // closing session if there is a RemoteException
                    throw (RemoteException) e;
                }
            }
        } finally {
            if (sessionID != null && closeSession) {
                sessionProvider.removeSessionSessionObject(sessionID);
            }
        }
    }

    private String getSessionID(JSONObject jsonObject) {
        String sessionID = jsonObject.optString("sessionID");
        return sessionID.isEmpty() ? "external" : sessionID;
    }
}
