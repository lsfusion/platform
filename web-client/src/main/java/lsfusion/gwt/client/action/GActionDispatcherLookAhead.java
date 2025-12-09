package lsfusion.gwt.client.action;

public interface GActionDispatcherLookAhead {
    GAction next();
    void drop();
}
