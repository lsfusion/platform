package lsfusion.server.form.instance;

public interface Instantiable<T> {
    T getInstance(InstanceFactory instanceFactory);
}
