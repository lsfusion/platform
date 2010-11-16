package platform.interop.context;

public interface ApplicationContextHolder extends ApplicationContextProvider {
    void setContext(ApplicationContext context);
}
