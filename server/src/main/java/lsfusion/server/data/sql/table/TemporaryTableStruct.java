package lsfusion.server.data.sql.table;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.table.KeyField;
import lsfusion.server.data.table.PropertyField;
import lsfusion.server.physics.admin.Settings;

// what makes two session tables interchangeable : the same keys, the same properties and - unless the statistics are taken after every fill - the same size bucket
public class TemporaryTableStruct {

    public final ImOrderSet<KeyField> keys;
    public final ImSet<PropertyField> properties;

    private final Object statistics;

    public TemporaryTableStruct(ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, Long count) {
        this.keys = keys;
        this.properties = properties;

        if(Settings.get().isAutoAnalyzeTempStats() || count==null)
            this.statistics = null;
        else
            this.statistics = getDBStatistics(count);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof TemporaryTableStruct && keys.equals(((TemporaryTableStruct) o).keys) && properties.equals(((TemporaryTableStruct) o).properties) && BaseUtils.nullEquals(statistics, ((TemporaryTableStruct) o).statistics);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * keys.hashCode() + properties.hashCode()) + BaseUtils.nullHash(statistics);
    }

    public static Object getDBStatistics(long count) {
        return new Stat(count);
    }
}
