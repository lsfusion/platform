package lsfusion.server.physics.dev.integration.external.to.net;

import com.google.common.base.Throwables;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.external.to.net.client.PingClientAction;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.net.InetAddress;
import java.util.Iterator;

public class PingAction extends InternalAction {
    private final ClassPropertyInterface hostInterface;
    private final ClassPropertyInterface isClientInterface;

    public PingAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        hostInterface = i.next();
        isClientInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        String host = (String) context.getKeyValue(hostInterface).getValue();
        boolean isClient = context.getKeyValue(isClientInterface).getValue() != null;

        try {
            String result;
            if (host != null) {
                if (isClient) {
                    result = (String) context.requestUserInteraction(new PingClientAction(host));
                } else {
                    result = InetAddress.getByName(host).isReachable(5000) ? null : "Host is not reachable";
                }
            } else {
                result = "no host";
            }
            findProperty("pingError[]").change(result, context);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}