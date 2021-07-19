package lsfusion.server.physics.dev.integration.external.to;

import com.google.common.base.Throwables;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.interop.session.UdpClientAction;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExternalUDPAction extends ExternalAction {
    boolean clientAction;
    private PropertyInterface queryInterface;

    public ExternalUDPAction(boolean clientAction, ImList<Type> params) {
        super(1, params, ListFact.EMPTY());

        this.clientAction = clientAction;
        this.queryInterface = getOrderInterfaces().get(0);
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) {
        try {
            Object file = context.getKeyObject(paramInterfaces.single());
            if (file instanceof RawFileData) {
                String connectionString = getTransformedText(context, queryInterface);
                if (connectionString != null) {
                    Pattern pattern = Pattern.compile("(.*):(\\d+)");
                    Matcher matcher = pattern.matcher(connectionString);
                    if (matcher.matches()) {
                        byte[] fileBytes = ((RawFileData) file).getBytes();
                        String host = matcher.group(1);
                        Integer port = Integer.parseInt(matcher.group(2));
                        if (clientAction) {
                            context.requestUserInteraction(new UdpClientAction(fileBytes, host, port));
                        } else {
                            ExternalUtils.sendUDP(fileBytes, host, port);
                        }
                    } else {
                        throw new RuntimeException(String.format("Failed to parse connectionString %s, use format: host:port", connectionString));
                    }
                } else {
                    throw new RuntimeException("file or connectionString not specified");
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        return FlowResult.FINISH;
    }
}