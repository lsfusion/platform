package lsfusion.server.logics.classes.data.time;

import lsfusion.server.data.type.DBType;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.TextBasedClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class TimeSeriesClass<T> extends TextBasedClass<T> implements DBType {

    public TimeSeriesClass(LocalizedString caption) {
        super(caption);
    }

    @Override
    public DBType getDBType() {
        return this;
    }

    public abstract String getIntervalProperty();
    public abstract String getFromIntervalProperty();
    public abstract String getToIntervalProperty();
}
