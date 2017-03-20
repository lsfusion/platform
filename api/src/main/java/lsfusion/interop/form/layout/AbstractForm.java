package lsfusion.interop.form.layout;

public interface AbstractForm<C extends AbstractContainer<T, Str>, T extends AbstractComponent, Str> {
    C getMainContainer();
}
