package lsfusion.server.logics.table;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.SystemClass;
import lsfusion.server.data.GlobalTable;
import lsfusion.server.data.KeyField;
import lsfusion.server.data.PropertyField;
import lsfusion.server.data.expr.query.PropStat;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.where.classes.ClassWhere;

public class DumbTable extends GlobalTable {

    public static final DumbTable instance = new DumbTable();

    public KeyField key;

    private DumbTable() {
        super("dumb");
        key = new KeyField("id", SystemClass.instance);
        keys = SetFact.singletonOrder(key);

        classes = new ClassWhere<>(key, SystemClass.instance);
    }

    public StatKeys<KeyField> getStatKeys() {
        return new StatKeys<>(keys.getSet(), Stat.ONE); // throw new RuntimeException("not supported");
    }

    public ImMap<PropertyField,PropStat> getStatProps() {
        throw new RuntimeException("not supported");
    }
}
