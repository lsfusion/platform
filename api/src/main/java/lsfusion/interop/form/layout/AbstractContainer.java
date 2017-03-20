package lsfusion.interop.form.layout;

public interface AbstractContainer<T extends AbstractComponent, Str> extends AbstractComponent {

    void setCaption(Str caption);
    void setDescription(Str description);
    void setSID(String sID);
    void setType(ContainerType type);
    void setChildrenAlignment(Alignment childrenAlignment);
    void setColumns(int columns);

    void add(T child);
}
