package platform.base.context;

import platform.base.context.ApplicationContext;
import platform.base.context.ApplicationContextHolder;

public class ContextObject implements ApplicationContextHolder {

    protected ApplicationContext context;

    public ApplicationContext getContext() {
        return context;
    }

    public void setContext(ApplicationContext context) {
        this.context = context;
    }

    public void updateDependency(Object object, String field) {
        context.updateDependency(object, field);
    }
}
