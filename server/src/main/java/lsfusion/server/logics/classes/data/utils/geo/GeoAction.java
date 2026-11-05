package lsfusion.server.logics.classes.data.utils.geo;

import lsfusion.interop.session.ExternalHttpMethod;
import lsfusion.interop.session.ExternalHttpResponse;
import lsfusion.interop.session.ExternalHttpUtils;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;

public class GeoAction extends InternalAction {

    private static final long GOOGLE_GEOCODE_TIMEOUT = 60000;

    public GeoAction(ScriptingLogicsModule LM) {
        super(LM);
    }

    public GeoAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLHandledException {
    }

    protected boolean isYandex(ExecutionContext context, DataObject mapProvider) throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {
        boolean isYandex = true;
        if (mapProvider != null) {
            String providerName = ((String) findProperty("staticName[StaticObject]").read(context, mapProvider));
            isYandex = providerName == null || providerName.contains("yandex");
        }
        return isYandex;
    }

    protected static JSONArray readGoogleGeocode(String params, String apiKey) throws IOException, JSONException {
        ExternalHttpResponse response = ExternalHttpUtils.sendRequest(ExternalHttpMethod.GET, "https://maps.googleapis.com/maps/api/geocode/json?" + params + "&key=" + apiKey,
                GOOGLE_GEOCODE_TIMEOUT, false, null, Collections.emptyMap(), Collections.emptyMap(), new BasicCookieStore());
        if (response.statusCode != 200 || response.responseBytes == null)
            throw new RuntimeException("Google geocoding failed: " + response.statusCode + " " + response.statusText);

        JSONObject result = new JSONObject(new String(response.responseBytes, ExternalUtils.jsonCharset));
        String status = result.getString("status");
        if (!status.equals("OK") && !status.equals("ZERO_RESULTS"))
            throw new RuntimeException("Google geocoding failed: " + status + (result.has("error_message") ? ", " + result.getString("error_message") : ""));
        return result.getJSONArray("results");
    }
}
