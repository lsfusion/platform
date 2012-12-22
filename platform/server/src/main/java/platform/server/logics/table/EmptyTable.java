package platform.server.logics.table;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.server.classes.SystemClass;
import platform.server.data.GlobalTable;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.stat.StatKeys;

public class EmptyTable extends GlobalTable {
    public static final EmptyTable instance = new EmptyTable();

    public KeyField key;

    public EmptyTable() {
        super("empty");
        keys = SetFact.singletonOrder(new KeyField("id", SystemClass.instance));
    }

    public StatKeys<KeyField> getStatKeys() {
        throw new RuntimeException("not supported");
    }

    public ImMap<PropertyField, Stat> getStatProps() {
        throw new RuntimeException("not supported");
    }
}
