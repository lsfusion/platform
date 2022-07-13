package lsfusion.server.logics.classes.data.time;

import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class TimeSeriesClass<T> extends DataClass<T> {

    public TimeSeriesClass(LocalizedString caption) {
        super(caption);
    }

    public abstract String getIntervalProperty();
    public abstract String getFromIntervalProperty();
    public abstract String getToIntervalProperty();
}
