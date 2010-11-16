package platform.client.descriptor.context;

import platform.client.descriptor.IdentityDescriptor;
import platform.interop.context.ApplicationContext;
import platform.interop.context.ApplicationContextHolder;
import platform.interop.context.IncrementView;

public class ContextIdentityDescriptor extends IdentityDescriptor implements ApplicationContextHolder {

    protected ApplicationContext context;

    public ApplicationContext getContext() {
        return context;
    }

    public void setContext(ApplicationContext context) {
        this.context = context;
    }

    public ContextIdentityDescriptor() {
    }

    public ContextIdentityDescriptor(ApplicationContext context) {
        super(context.idShift());
        this.context = context;
    }

    public ContextIdentityDescriptor(int ID) {
        this(ID, null);
    }

    public ContextIdentityDescriptor(int ID, ApplicationContext context) {
        super(ID);
        this.context = context;
    }

    public void addDependency(String field, IncrementView view) {
        context.addDependency(field, view);
    }

    public void addDependency(Object object, String field, IncrementView view) {
        context.addDependency(object, field, view);
    }

    public void removeDependency(String field, IncrementView view) {
        context.removeDependency(field, view);
    }

    public void removeDependency(Object object, String field, IncrementView view) {
        context.removeDependency(object, field, view);
    }

    public void updateDependency(Object object, String field) {
        context.updateDependency(object, field);
    }
}
