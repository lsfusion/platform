package lsfusion.interop.form.layout;

public interface AbstractGroupObject<T extends AbstractComponent, Str> {

    Str getCaption();
    int getID();
    String getSID();

    T getGrid();
    T getShowType();
    T getToolbar();
    T getFilter();
    T getCalculations();
}
