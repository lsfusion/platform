package lsfusion.server.physics.exec.table;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.SystemClass;
import lsfusion.server.data.DBTable;
import lsfusion.server.data.KeyField;
import lsfusion.server.data.PropertyField;
import lsfusion.server.data.expr.query.PropStat;
import lsfusion.server.data.query.stat.TableStatKeys;
import lsfusion.server.data.where.classes.ClassWhere;

public class DumbTable extends DBTable {

    public static final DumbTable instance = new DumbTable();

    public KeyField key;

    private DumbTable() {
        super("dumb");
        key = new KeyField("id", SystemClass.instance);
        keys = SetFact.singletonOrder(key);

        classes = new ClassWhere<>(key, SystemClass.instance);
    }

    public TableStatKeys getTableStatKeys() {
        return TableStatKeys.createForTable(1, MapFact.singleton(key, 1)); // throw new RuntimeException("not supported");
    }

    public ImMap<PropertyField,PropStat> getStatProps() {
        return MapFact.EMPTY();
    }
}
