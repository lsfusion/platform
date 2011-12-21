package platform.server;

import platform.server.logics.BusinessLogics;

public interface Context {
    ThreadLocal<Context> context = new ThreadLocal<Context>();

    void setActionMessage(String message);
    String getActionMessage();
    void pushActionMessage(String segment);
    String popActionMessage();
    BusinessLogics getBL();
}
