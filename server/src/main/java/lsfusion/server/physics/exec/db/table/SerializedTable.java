package lsfusion.server.physics.exec.db.table;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.stat.PropStat;
import lsfusion.server.data.stat.TableStatKeys;
import lsfusion.server.data.table.KeyField;
import lsfusion.server.data.table.NamedTable;
import lsfusion.server.data.table.PropertyField;
import lsfusion.server.data.table.Table;
import lsfusion.server.logics.classes.user.BaseClass;

import java.io.DataInputStream;
import java.io.IOException;

// предыдущая таблица в базе
public class SerializedTable extends NamedTable {

    public SerializedTable(String dbName, String canonicalName, DataInputStream inStream, BaseClass baseClass) throws IOException {
        super(inStream, dbName, baseClass);
        this.canonicalName = canonicalName;
    }

    private final static int prevStats = 100000;
    public static TableStatKeys getStatKeys(Table table) {
        return getStatKeys(table, prevStats);
    }
    public static ImMap<PropertyField, PropStat> getStatProps(SerializedTable table) {
        return getStatProps((Table)table);
    }

    public TableStatKeys getTableStatKeys() {
        return getStatKeys(this);
    }

    public ImMap<PropertyField,PropStat> getStatProps() {
        return getStatProps(this);
    }
}
