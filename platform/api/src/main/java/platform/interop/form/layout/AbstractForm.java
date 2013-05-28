package platform.interop.form.layout;

public interface AbstractForm<C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>> {
    C getMainContainer();
}
