package lsfusion.server.logics.action.session;

import lsfusion.interop.action.ClientAction;

public interface UserInteraction {

    void delayUserInteraction(ClientAction action);

    Object requestUserInteraction(ClientAction action);

}
