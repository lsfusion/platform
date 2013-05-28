package platform.interop.form.layout;

public interface AbstractGroupObject<T extends AbstractComponent> {

    String getCaption();
    int getID();
    String getSID();

    T getGrid();
    T getShowType();
    T getToolbar();
    T getFilter();
}
