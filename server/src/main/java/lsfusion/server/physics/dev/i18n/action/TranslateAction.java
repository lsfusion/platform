package lsfusion.server.physics.dev.i18n.action;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TranslateAction extends InternalAction {
    public final ClassPropertyInterface translationInterface;
    public final ClassPropertyInterface languageFromInterface;
    public final ClassPropertyInterface languageToInterface;

    public TranslateAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        translationInterface = i.next();
        languageFromInterface = i.next();
        languageToInterface = i.next();
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {

            ObjectValue translationEntry = context.getKeyValue(translationInterface);
            ObjectValue languageFromObject = context.getKeyValue(languageFromInterface);
            ObjectValue languageToObject = context.getKeyValue(languageToInterface);

            String apiKey = (String) findProperty("translateApiKey[]").read(context);

            if (languageToObject != null && translationEntry != null) {

                String languageFrom = (String) findProperty("locale[Language]").read(context, languageFromObject);
                String languageTo = (String) findProperty("locale[Language]").read(context, languageToObject);

                if(languageTo != null) {
                    CloseableHttpClient httpclient = HttpClients.createDefault();
                    HttpPost httppost = new HttpPost("https://translation.googleapis.com/language/translate/v2");

                    List<NameValuePair> params = new ArrayList<NameValuePair>(2);
                    params.add(new BasicNameValuePair("q", (String) translationEntry.getValue()));
                    if (languageFrom != null)
                        params.add(new BasicNameValuePair("source", (String) languageFrom));
                    params.add(new BasicNameValuePair("target", (String) languageTo));
                    if (apiKey != null)
                        params.add(new BasicNameValuePair("key", (String) apiKey));

                    httppost.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));

                    CloseableHttpResponse response = httpclient.execute(httppost);
                    findProperty("translationCode[]").change(response.getCode(), context);

                    HttpEntity entity = response.getEntity();

                    String result = null;
                    if (entity != null) {
                        try (InputStream instream = entity.getContent()) {
                            BufferedReader streamReader = new BufferedReader(new InputStreamReader(instream, StandardCharsets.UTF_8));
                            StringBuilder responseStrBuilder = new StringBuilder();

                            String inputStr;
                            while ((inputStr = streamReader.readLine()) != null)
                                responseStrBuilder.append(inputStr);

                            String responseString = responseStrBuilder.toString();
                            JSONObject jsonObject = new JSONObject(responseString);

                            if (jsonObject.has("data")) {
                                JSONObject data = jsonObject.getJSONObject("data");

                                JSONArray translations = data.getJSONArray("translations");
                                result = (String) translations.getJSONObject(0).get("translatedText");
                            } else {
                                result = responseString;
                            }
                        }
                    }

                    findProperty("translationResult[]").change(result, context);
                }
            }

        } catch (ScriptingErrorLog.SemanticErrorException | IOException ignored) {
        }

    }
}
