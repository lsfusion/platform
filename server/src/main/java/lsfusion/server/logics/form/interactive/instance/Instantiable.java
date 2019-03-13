package lsfusion.server.logics.form.interactive.instance;

public interface Instantiable<T> {
    T getInstance(InstanceFactory instanceFactory);
}
