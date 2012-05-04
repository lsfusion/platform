package platform.interop.form.layout;

public interface AbstractFunction<C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>> extends AbstractComponent<C,T> {

    void setCaption(String caption);
    void setIconPath(String iconPath);
    void setType(String type);
}
