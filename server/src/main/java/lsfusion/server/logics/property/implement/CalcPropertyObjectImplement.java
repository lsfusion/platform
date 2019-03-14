package lsfusion.server.logics.property.implement;

public class CalcPropertyObjectImplement<T> implements CalcPropertyObjectInterfaceImplement<T> {

    public final T object;

    public CalcPropertyObjectImplement(T object) {
        this.object = object;
    }
}
