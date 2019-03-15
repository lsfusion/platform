package lsfusion.server.logics.form.interactive.controller.init;

public interface Instantiable<T> {
    T getInstance(InstanceFactory instanceFactory);
}
