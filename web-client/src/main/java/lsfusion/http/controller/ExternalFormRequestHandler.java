package lsfusion.http.controller;

import lsfusion.base.Pair;
import lsfusion.base.file.IOUtils;
import lsfusion.http.provider.form.FormProvider;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.http.provider.navigator.NavigatorProvider;
import lsfusion.http.provider.navigator.NavigatorSessionObject;
import lsfusion.interop.form.remote.RemoteFormInterface;
import lsfusion.interop.logics.LogicsRunnable;
import lsfusion.interop.logics.LogicsSessionObject;
import lsfusion.interop.session.ExternalUtils;
import org.apache.hc.core5.http.ContentType;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.Charset;
import java.rmi.RemoteException;

public class ExternalFormRequestHandler extends ExternalRequestHandler {

    public ExternalFormRequestHandler(LogicsProvider logicsProvider, NavigatorProvider logicsAndNavigatorProvider, FormProvider formProvider) {
        super(logicsProvider);
        this.logicsAndNavigatorProvider = logicsAndNavigatorProvider;
        this.formProvider = formProvider;
    }

    private final NavigatorProvider logicsAndNavigatorProvider;
    private final FormProvider formProvider;

    @Override
    protected void handleRequest(LogicsSessionObject sessionObject, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ContentType contentType = ExternalUtils.parseContentType(request.getContentType());

        Charset charset = ExternalUtils.getBodyCharset(contentType);
        String json = new String(IOUtils.readBytesFromStream(request.getInputStream()), charset);
        JSONObject jsonObject = new JSONObject(json);

        String jsonResult;
        String action = jsonObject.getString("action");

        if (action.equals("genids")) {
            final int count = jsonObject.getInt("count");
            jsonResult = new JSONArray(runRequest(request, (LogicsRunnable<Object>) (sessionObject1, retry) -> {
                long[] ids = new long[count];
                for(int i = 0; i< count; i++)
                    ids[i] = sessionObject1.remoteLogics.generateID();
                return ids;
            })).toString();
        } else {
            String formID = jsonObject.getString("form");

            JSONObject dataObject = jsonObject.optJSONObject("data");
            String data = dataObject != null ? dataObject.toString() : null;

            if (action.equals("create")) {
                String navigatorID = jsonObject.optString("navigator");
                boolean useGlobalNavigator = false;
                if (navigatorID.isEmpty()) {
                    navigatorID = "external";
                    useGlobalNavigator = true;
                }

                NavigatorSessionObject navigatorSessionObject = logicsAndNavigatorProvider.createOrGetNavigatorSessionObject(navigatorID, sessionObject, request);

                Pair<RemoteFormInterface, String> result;
                try {
                    result = navigatorSessionObject.remoteNavigator.createFormExternal(data);
                } catch (RemoteException e) {
                    if (useGlobalNavigator) { // we need to invalidate all refs, otherwise we will always get no such object
                        formProvider.removeFormSessionObjects(navigatorID);
                        logicsAndNavigatorProvider.removeNavigatorSessionObject(navigatorID);
                    }
                    throw e;
                }
                formProvider.createFormExternal(formID, result.first, navigatorID); // registering form for further usage
                jsonResult = result.second;
            } else {
                FormSessionObject formSessionObject = formProvider.getFormSessionObject(formID);
                if (action.equals("change")) {
                    Pair<Long, String> result = formSessionObject.remoteForm.changeExternal(jsonObject.getLong("requestIndex"), jsonObject.getLong("lastReceivedRequestIndex"), data);
                    jsonResult = result.second;
                } else {
                    formSessionObject.remoteForm.close();
                    formProvider.scheduleRemoveFormSessionObject(formID, 1000);
                    jsonResult = "{}";
                }
            }
        }

        sendResponse(response, new ExternalUtils.ResultExternalResponse(jsonResult, charset, HttpServletResponse.SC_OK));
    }
}
