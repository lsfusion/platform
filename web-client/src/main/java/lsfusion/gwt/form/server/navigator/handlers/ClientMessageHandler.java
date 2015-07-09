package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.gwt.base.server.dispatch.SimpleActionHandlerEx;
import lsfusion.gwt.form.server.FormDispatchServlet;
import lsfusion.gwt.form.shared.actions.navigator.ClientMessage;
import lsfusion.gwt.form.shared.actions.navigator.ClientMessageResult;
import lsfusion.interop.RemoteLogicsInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class ClientMessageHandler extends SimpleActionHandlerEx<ClientMessage, ClientMessageResult, RemoteLogicsInterface> {
    public ClientMessageHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ClientMessageResult executeEx(ClientMessage action, ExecutionContext context) throws DispatchException, IOException {
        return new ClientMessageResult(servlet.getLogics().getClientMessage());
    }
}
