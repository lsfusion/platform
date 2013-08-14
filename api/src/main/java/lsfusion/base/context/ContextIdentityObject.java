package lsfusion.base.context;

import lsfusion.base.identity.IdentityObject;

public class ContextIdentityObject extends IdentityObject implements ApplicationContextHolder {

    protected ApplicationContext context;

    public ApplicationContext getContext() {
        return context;
    }

    public void setContext(ApplicationContext context) {
        this.context = context;
    }

    public ContextIdentityObject() {
    }

    public ContextIdentityObject(ApplicationContext context) {
        super(context.idShift());
        this.context = context;
    }

    public ContextIdentityObject(int ID) {
        this(ID, null);
    }

    public ContextIdentityObject(int ID, ApplicationContext context) {
        super(ID);
        this.context = context;
    }

    public void addDependency(String field, IncrementView view) {
        if (context != null) {
            context.addDependency(field, view);
        }
    }

    public void addDependency(Object object, String field, IncrementView view) {
        if (context != null) {
            context.addDependency(object, field, view);
        }
    }

    public void removeDependency(String field, IncrementView view) {
        if (context != null) {
            context.removeDependency(field, view);
        }
    }

    public void removeDependency(IncrementView view) {
        if (context != null) {
            context.removeDependency(view);
        }
    }

    public void removeDependency(Object object, String field, IncrementView view) {
        if (context != null) {
            context.removeDependency(object, field, view);
        }
    }

    public void updateDependency(Object object, String field) {
        if (context != null) {
            context.updateDependency(object, field);
        }
    }
}
