package lsfusion.server.physics.dev.integration.external.to;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.interop.session.UdpClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.io.IOException;
import java.sql.SQLException;

public class ExternalUDPAction extends ExternalSocketAction {
    public ExternalUDPAction(boolean clientAction, ImList<Type> params) {
        super(clientAction, params);
    }

    @Override
    protected void send(ExecutionContext<PropertyInterface> context, String host, Integer port, byte[] fileBytes) throws SQLException, SQLHandledException, IOException {
        if (clientAction) {
            context.requestUserInteraction(new UdpClientAction(fileBytes, host, port));
        } else {
            ExternalUtils.sendUDP(fileBytes, host, port);
        }
    }
}