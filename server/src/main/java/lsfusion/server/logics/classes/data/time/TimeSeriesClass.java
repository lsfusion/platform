package lsfusion.server.logics.classes.data.time;

import lsfusion.base.BaseUtils;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.Collection;
import java.util.function.Supplier;

public abstract class TimeSeriesClass<T> extends DataClass<T> {
    public final ExtInt millisLength;

    public TimeSeriesClass(LocalizedString caption, ExtInt millisLength) {
        super(caption);
        this.millisLength = millisLength;
    }

    public abstract String getIntervalProperty();
    public abstract String getFromIntervalProperty();
    public abstract String getToIntervalProperty();

    protected static <S extends TimeSeriesClass<?>> S getCached(Collection<S> timeSeriesClasses, ExtInt millisLength, Supplier<S> constructor) {
        synchronized (timeSeriesClasses) {
            for (S timeSeriesClass : timeSeriesClasses) {
                if (BaseUtils.nullEquals(timeSeriesClass.millisLength, millisLength))
                    return timeSeriesClass;
            }

            S timeSeriesClass = constructor.get();
            timeSeriesClasses.add(timeSeriesClass);
            return timeSeriesClass;
        }
    }
}
