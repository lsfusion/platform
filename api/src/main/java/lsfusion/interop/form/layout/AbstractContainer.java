package lsfusion.interop.form.layout;

public interface AbstractContainer<C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>> extends AbstractComponent<C, T> {

    void setCaption(String caption);
    void setDescription(String description);
    void setSID(String sID);
    void setType(byte type);

    void add(T child);
}
