package lsfusion.interop.form.layout;

public interface AbstractComponent<C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>> {

    SimplexConstraints<T> getConstraints();
    void setFlex(double flex);
    void setAlignment(FlexAlignment alignment);
}
