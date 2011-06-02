package platform.interop.form.layout;

public interface AbstractGroupObject<T extends AbstractComponent> {

    String getCaption();
    int getID();

    T getGrid();
    T getShowType();
}
