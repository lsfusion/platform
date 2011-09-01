package platform.server;

public interface Context {
    ThreadLocal<Context> context = new ThreadLocal<Context>();

    void setActionMessage(String message);
    String getActionMessage();
    void pushActionMessage(String segment);
    String popActionMessage();
}
