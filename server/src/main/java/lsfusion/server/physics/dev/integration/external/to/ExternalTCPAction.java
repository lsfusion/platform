package lsfusion.server.physics.dev.integration.external.to;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.interop.session.TcpClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.io.IOException;
import java.sql.SQLException;

public class ExternalTCPAction extends ExternalSocketAction {
    public ExternalTCPAction(boolean clientAction, ImList<Type> params) {
        super(clientAction, params);
    }

    @Override
    protected void send(ExecutionContext<PropertyInterface> context, String host, Integer port, byte[] fileBytes) throws SQLException, SQLHandledException, IOException {
        Integer timeout = (Integer) context.getBL().LM.timeoutTcp.read(context);
        byte[] response;
        if (clientAction) {
            response = (byte[]) context.requestUserInteraction(new TcpClientAction(fileBytes, host, port, timeout));
        } else {
            response = ExternalUtils.sendTCP(fileBytes, host, port, timeout);
        }
        context.getBL().LM.responseTcp.change(new RawFileData(response), context);
    }
}