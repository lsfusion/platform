package lsfusion.server.physics.exec.db.table;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.stat.PropStat;
import lsfusion.server.data.stat.TableStatKeys;
import lsfusion.server.data.table.KeyField;
import lsfusion.server.data.table.PropertyField;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.classes.data.SystemClass;

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
        return TableStatKeys.createForTable(1L, MapFact.singleton(key, 1L)); // throw new RuntimeException("not supported");
    }

    public ImMap<PropertyField,PropStat> getStatProps() {
        return MapFact.EMPTY();
    }
}
