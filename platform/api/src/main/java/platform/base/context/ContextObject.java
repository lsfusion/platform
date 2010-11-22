package platform.base.context;

public class ContextObject implements ApplicationContextHolder {

    protected ApplicationContext context;

    public ContextObject(){
    }

    public ContextObject(ApplicationContext context){
        this.context = context;  
    }

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
