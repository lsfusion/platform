package platform.server.data;

import platform.base.col.interfaces.immutable.ImMap;
import platform.server.classes.BaseClass;
import platform.server.data.expr.query.PropStat;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.stat.StatKeys;

import java.io.DataInputStream;
import java.io.IOException;

// предыдущая таблица в базе
public class SerializedTable extends Table {

    public SerializedTable(DataInputStream inStream, BaseClass baseClass, int version) throws IOException {
        super(inStream, baseClass, version);
    }

    private final static int prevStats = 100000;
    public static StatKeys<KeyField> getStatKeys(Table table) {
        return getStatKeys(table, prevStats);
    }
    public static ImMap<PropertyField, PropStat> getStatProps(Table table) {
        return getStatProps(table, prevStats);
    }

    public StatKeys<KeyField> getStatKeys() {
        return getStatKeys(this);
    }

    public ImMap<PropertyField,PropStat> getStatProps() {
        return getStatProps(this);
    }
}
