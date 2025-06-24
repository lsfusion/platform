package lsfusion.server.logics.classes.data.utils;

import lsfusion.base.file.StringWithFiles;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.changed.FormChanges;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.monitor.SystemEventsLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.security.*;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

public class PushAction extends InternalAction {
    private final ClassPropertyInterface notifyInterface;
    private final ClassPropertyInterface notificationInterface;
    private final ClassPropertyInterface actionInterface;
    private final ClassPropertyInterface inputActionsInterface;
    private final ClassPropertyInterface pushInterface;

    public PushAction(SystemEventsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        notifyInterface = i.next();
        notificationInterface = i.next();
        actionInterface = i.next();
        inputActionsInterface = i.next();
        pushInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
                Security.addProvider(new BouncyCastleProvider());

            JSONObject payload = new JSONObject();
            Boolean notify = (Boolean) context.getKeyValue(notifyInterface).getValue();
            if(notify != null)
                payload.put("notify", notify);
            String notification = (String) context.getKeyValue(notificationInterface).getValue();
            payload.put("notification", notification != null ? new JSONObject(notification) : null);

            payload.put("action", new JSONObject((String) context.getKeyValue(actionInterface).getValue()));
            payload.put("inputActions", new JSONArray((String) context.getKeyValue(inputActionsInterface).getValue()));
            JSONObject pushJson = new JSONObject((String) context.getKeyValue(pushInterface).getValue());
            payload.put("push", pushJson);

            getPushService(context).send(new Notification(getSubscription(pushJson),
                    (String) context.convertFileValue(payload.toString())), Encoding.AES128GCM); // Encoding.AES128GCM important!

        } catch (JoseException | GeneralSecurityException | IOException | ExecutionException | InterruptedException |
                 ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        }
    }

    private PushService getPushService(ExecutionContext<ClassPropertyInterface> context) throws GeneralSecurityException, SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {
        return new PushService((String) findProperty("pushPublicKey[]").read(context), (String) findProperty("pushPrivateKey[]").read(context));
    }

    private static Subscription getSubscription(JSONObject pushJson) {
        JSONObject subscriptionJson = pushJson.optJSONObject("subscription");
        JSONObject keysJson = subscriptionJson.optJSONObject("keys");
        return new Subscription(subscriptionJson.optString("endpoint"), new Subscription.Keys(keysJson.optString("p256dh"), keysJson.optString("auth")));
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}
