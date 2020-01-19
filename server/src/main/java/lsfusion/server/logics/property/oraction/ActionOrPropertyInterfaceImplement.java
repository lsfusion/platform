package lsfusion.server.logics.property.oraction;

// needed for readImplements(Object...) when there are properties and actions (in If, For and so on) + + implicit abstracts
public interface ActionOrPropertyInterfaceImplement<P extends PropertyInterface> {

    // потому как используется в одном месте
    boolean equalsMap(ActionOrPropertyInterfaceImplement<P> object);
    int hashMap();
}
