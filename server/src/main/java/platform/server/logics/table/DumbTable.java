package platform.server.logics.table;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.server.classes.SystemClass;
import platform.server.data.GlobalTable;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.expr.query.PropStat;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.where.classes.ClassWhere;

public class DumbTable extends GlobalTable {

    public static final DumbTable instance = new DumbTable();

    public KeyField key;

    private DumbTable() {
        super("dumb");
        key = new KeyField("id", SystemClass.instance);
        keys = SetFact.singletonOrder(key);

        classes = new ClassWhere<KeyField>(key, SystemClass.instance);
    }

    public StatKeys<KeyField> getStatKeys() {
        throw new RuntimeException("not supported");
    }

    public ImMap<PropertyField,PropStat> getStatProps() {
        throw new RuntimeException("not supported");
    }
}
