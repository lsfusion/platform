package platform.server.form.instance;

import platform.server.form.instance.filter.FilterInstance;

public interface Instantiable<T> {
    T getInstance(InstanceFactory instanceFactory);
}
