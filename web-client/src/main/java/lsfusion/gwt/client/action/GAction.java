package lsfusion.gwt.client.action;

import java.io.Serializable;

public interface GAction extends Serializable {
    Object dispatch(GActionDispatcher dispatcher) throws Throwable;
}
