package lsfusion.server.logics.table;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.SystemClass;
import lsfusion.server.data.GlobalTable;
import lsfusion.server.data.KeyField;
import lsfusion.server.data.PropertyField;
import lsfusion.server.data.expr.query.PropStat;
import lsfusion.server.data.query.stat.StatKeys;

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

    public ImMap<PropertyField,PropStat> getStatProps() {
        throw new RuntimeException("not supported");
    }
}
