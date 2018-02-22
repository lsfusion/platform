package lsfusion.interop.form.layout;

public interface AbstractTreeGroup<T extends AbstractComponent> extends AbstractComponent {

    int getID();
    String getSID();

    T getToolbarSystem();
    T getUserFilter();
}
