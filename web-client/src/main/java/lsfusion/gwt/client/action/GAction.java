package lsfusion.gwt.client.action;

import java.io.Serializable;

public interface GAction extends Serializable {
    Object dispatch(GActionDispatcher dispatcher) throws Throwable;

    default Object dispatch(GActionDispatcher dispatcher, GActionDispatcherLookAhead lookAhead) throws Throwable {
        return dispatch(dispatcher);
    }
}
