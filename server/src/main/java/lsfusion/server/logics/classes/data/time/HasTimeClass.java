package lsfusion.server.logics.classes.data.time;

import lsfusion.base.BaseUtils;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.Collection;
import java.util.function.Supplier;

public abstract class HasTimeClass<T> extends TimeSeriesClass<T> {
    public final ExtInt millisLength;

    public HasTimeClass(LocalizedString caption, ExtInt millisLength) {
        super(caption);
        this.millisLength = millisLength;
    }

    protected static <S extends HasTimeClass<?>> S getCached(Collection<S> hasTimeClasses, ExtInt millisLength, Supplier<S> constructor) {
        synchronized (hasTimeClasses) {
            for (S hasTimeClass : hasTimeClasses) {
                if (BaseUtils.nullEquals(hasTimeClass.millisLength, millisLength))
                    return hasTimeClass;
            }

            S hasTimeSeriesClass = constructor.get();
            hasTimeClasses.add(hasTimeSeriesClass);
            return hasTimeSeriesClass;
        }
    }
}
