package lsfusion.server.physics.exec.db.table;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.stat.PropStat;
import lsfusion.server.data.stat.TableStatKeys;
import lsfusion.server.data.table.PropertyField;
import lsfusion.server.data.table.StoredTable;
import lsfusion.server.logics.classes.user.BaseClass;

import java.io.DataInputStream;
import java.io.IOException;

// предыдущая таблица в базе
public class SerializedTable extends DBTable {

    public SerializedTable(String dbName, String canonicalName, DataInputStream inStream, BaseClass baseClass) throws IOException {
        super(inStream, dbName, baseClass);
        this.canonicalName = canonicalName;
    }

    private final static int prevStats = 100000;
    public static TableStatKeys getStatKeys(StoredTable table) {
        return getStatKeys(table, prevStats);
    }

    public TableStatKeys getTableStatKeys() {
        return getStatKeys(this);
    }

    @IdentityLazy
    public ImMap<PropertyField,PropStat> getStatProps() {
        return getStatProps(this);
    }
}
