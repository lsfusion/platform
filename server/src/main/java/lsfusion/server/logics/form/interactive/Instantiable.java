package lsfusion.server.logics.form.interactive;

public interface Instantiable<T> {
    T getInstance(InstanceFactory instanceFactory);
}
