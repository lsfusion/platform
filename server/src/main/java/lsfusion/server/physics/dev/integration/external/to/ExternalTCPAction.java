package lsfusion.server.physics.dev.integration.external.to;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.interop.session.TcpClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.action.controller.context.ConnectionService;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.io.IOException;
import java.net.Socket;
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
            Socket socket = null;
            ConnectionService connectionService = context.getConnectionService();
            if (connectionService != null) {
                socket = connectionService.getTCPSocket(host, port);
            }
            if (socket == null) {
                socket = new Socket(host, port);
                if (connectionService != null)
                    connectionService.putTCPSocket(host, port, socket);
            }
            try {
                response = ExternalUtils.sendTCP(fileBytes, socket, timeout, connectionService != null);
            } finally {
                if (connectionService == null)
                    socket.close();
            }
        }
        context.getBL().LM.responseTcp.change(new RawFileData(response), context);
    }
}