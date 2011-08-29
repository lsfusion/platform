package platform.server.logics.table;

import platform.server.classes.SystemClass;
import platform.server.data.KeyField;
import platform.server.data.GlobalTable;
import platform.server.data.PropertyField;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.stat.StatKeys;

import java.util.Map;

public class EmptyTable extends GlobalTable {
    public static final EmptyTable instance = new EmptyTable();

    public KeyField key;

    public EmptyTable() {
        super("empty");
        keys.add(new KeyField("id", SystemClass.instance));
    }

    public StatKeys<KeyField> getStatKeys() {
        throw new RuntimeException("not supported");
    }

    public Map<PropertyField, Stat> getStatProps() {
        throw new RuntimeException("not supported");
    }
}
