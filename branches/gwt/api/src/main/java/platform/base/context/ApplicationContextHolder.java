package platform.base.context;

public interface ApplicationContextHolder extends ApplicationContextProvider {
    void setContext(ApplicationContext context);
}
