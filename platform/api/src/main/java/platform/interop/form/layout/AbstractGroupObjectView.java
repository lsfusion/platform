package platform.interop.form.layout;

public interface AbstractGroupObjectView<T extends AbstractComponent> {

    String getCaption();
    int getID();

    T getGrid();
    T getShowType();
}
