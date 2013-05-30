package platform.interop.form.layout;

public interface AbstractTreeGroup<C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>> extends AbstractComponent<C,T> {

    int getID();
    String getSID();

    T getToolbar();
    T getFilter();
}
