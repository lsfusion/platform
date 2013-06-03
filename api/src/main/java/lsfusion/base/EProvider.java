package lsfusion.base;

import com.google.common.base.Throwables;

public abstract class EProvider<T> implements Provider<T> {
    @Override
    public T get() {
        try {
            return getExceptionally();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public abstract T getExceptionally() throws Exception;
}
