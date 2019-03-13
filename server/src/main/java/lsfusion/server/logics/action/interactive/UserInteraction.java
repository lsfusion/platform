package lsfusion.server.logics.action.interactive;

import lsfusion.interop.action.ClientAction;

public interface UserInteraction {

    void delayUserInteraction(ClientAction action);

    Object requestUserInteraction(ClientAction action);

}
