package lsfusion.server.logics.classes.data.utils;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.monitor.SystemEventsLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.*;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

public class WebPushAction extends InternalAction {
    private final ClassPropertyInterface subscriptionInterface;
    private final ClassPropertyInterface payloadInterface;

    public WebPushAction(SystemEventsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        subscriptionInterface = i.next();
        payloadInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
                Security.addProvider(new BouncyCastleProvider());

            String publicKey = (String) findProperty("pushNotificationPublicKey[]").read(context);
            String privateKey = (String) findProperty("pushNotificationPrivateKey[]").read(context);
            String payload = (String) context.getKeyValue(payloadInterface).getValue();

            JSONObject subscriptionJson = new JSONObject((String) context.getKeyValue(subscriptionInterface).getValue());
            JSONObject keysJson = subscriptionJson.optJSONObject("keys");

            String endpoint = subscriptionJson.optString("endpoint");
            String auth = keysJson.optString("auth");
            String key = keysJson.optString("p256dh");

            PushService pushService = new PushService(publicKey, privateKey);
            Subscription.Keys keys = new Subscription.Keys(key, auth);
            Subscription subscription = new Subscription(endpoint, keys);
            Notification notification = new Notification(subscription, payload);

            pushService.send(notification, Encoding.AES128GCM); // Encoding.AES128GCM important!

        } catch (JoseException | GeneralSecurityException | IOException | ExecutionException | InterruptedException |
                 ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        }
    }
}
