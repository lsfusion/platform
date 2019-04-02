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
    FormProvider formProvider;

    @Override
    protected void handleRequest(LogicsSessionObject sessionObject, HttpServletRequest request, HttpServletResponse response) throws RemoteException {
        try {
            String contentTypeString = request.getContentType();
            ContentType contentType = contentTypeString != null ? ContentType.parse(contentTypeString) : null;

            Charset charset = ExternalUtils.getCharsetFromContentType(contentType);
            String json = new String(IOUtils.readBytesFromStream(request.getInputStream()), charset);
            JSONObject jsonObject = new JSONObject(json);

            String jsonResult;
            String action = jsonObject.getString("action");
            
            if(action.equals("create")) {
                String navigatorID = jsonObject.getString("navigator");
                if(navigatorID == null)
                    navigatorID = "external";

                NavigatorSessionObject navigatorSessionObject = logicsAndNavigatorProvider.createOrGetNavigatorSessionObject(navigatorID, sessionObject, request);
                Pair<RemoteFormInterface, String> result = navigatorSessionObject.remoteNavigator.createFormExternal(jsonObject.getString("name"));
                jsonResult = addForm(formProvider.createFormExternal(result.first, navigatorID), result.second);
            } else {
                String formID = jsonObject.getString("form");
                
                FormSessionObject formSessionObject = formProvider.getFormSessionObject(formID);
                if(action.equals("change")) {
                    Pair<Long, String> result = formSessionObject.remoteForm.changeExternal(formSessionObject.requestIndex++, defaultLastReceivedRequestIndex, json);
                    jsonResult = result.second;
                } else {
                    formSessionObject.remoteForm.closeExternal();
                    formProvider.removeFormSessionObject(formID);
                    jsonResult = "{}";
                }
            }

            sendResponse(request, response, jsonResult, charset, false, true);
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

                if (e instanceof RemoteException) // rethrow RemoteException to invalidate LogicsSessionObject in LogicsProvider
                    throw (RemoteException) e;
            }
        }
    }

    private String addForm(String formID, String json) {
        JSONObject jsonResponse = new JSONObject(json);
        jsonResponse.put("form", formID);
        return jsonResponse.toString();
    }
}
