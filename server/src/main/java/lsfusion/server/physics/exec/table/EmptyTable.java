package lsfusion.server.physics.exec.table;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.SystemClass;
import lsfusion.server.data.DBTable;
import lsfusion.server.data.KeyField;
import lsfusion.server.data.PropertyField;
import lsfusion.server.data.expr.query.PropStat;
import lsfusion.server.data.query.stat.TableStatKeys;

public class EmptyTable extends DBTable {
    public static final EmptyTable instance = new EmptyTable();

    public KeyField key;

    public EmptyTable() {
        super("empty");
        keys = SetFact.singletonOrder(new KeyField("id", SystemClass.instance));
    }

    public TableStatKeys getTableStatKeys() {
        throw new RuntimeException("not supported");
    }

    public ImMap<PropertyField,PropStat> getStatProps() {
        throw new RuntimeException("not supported");
    }
}
