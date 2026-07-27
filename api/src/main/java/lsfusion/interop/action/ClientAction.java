package lsfusion.interop.action;

import java.io.IOException;
import java.io.Serializable;

public interface ClientAction extends Serializable {
    Object dispatch(ClientActionDispatcher dispatcher) throws IOException;

    // whether the client answers this action only after the USER does something - acknowledges a message, answers a question, enters a value, works with a modal form;
    // the actions the client performs by itself (reading a file, running a command, printing, downloading) block the server too, however long they take, but that is
    // an external call, a different class of problem
    default boolean isUserInteraction() {
        return false;
    }
}
