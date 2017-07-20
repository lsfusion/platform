package lsfusion.server.session;

import lsfusion.interop.action.ClientAction;
import lsfusion.server.context.ThreadLocalContext;

public interface UserInteraction {

    void delayUserInteraction(ClientAction action);

    Object requestUserInteraction(ClientAction action);

}
