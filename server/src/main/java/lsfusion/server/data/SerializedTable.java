package lsfusion.server.data;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.data.expr.query.PropStat;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.query.stat.TableStatKeys;

import java.io.DataInputStream;
import java.io.IOException;

// предыдущая таблица в базе
public class SerializedTable extends Table {

    public SerializedTable(DataInputStream inStream, BaseClass baseClass) throws IOException {
        super(inStream, baseClass);
    }

    public SerializedTable(String name, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, BaseClass baseClass) {
        super(name, keys, properties, null, null);
        initBaseClasses(baseClass);
    }

    private final static int prevStats = 100000;
    public static TableStatKeys getStatKeys(Table table) {
        return getStatKeys(table, prevStats);
    }
    public static ImMap<PropertyField, PropStat> getStatProps(Table table) {
        return getStatProps(table, prevStats);
    }

    public TableStatKeys getTableStatKeys() {
        return getStatKeys(this);
    }

    public ImMap<PropertyField,PropStat> getStatProps() {
        return getStatProps(this);
    }
}
