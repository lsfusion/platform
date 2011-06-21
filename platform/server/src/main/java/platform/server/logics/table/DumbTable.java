package platform.server.logics.table;

import platform.server.classes.SystemClass;
import platform.server.data.KeyField;
import platform.server.data.Table;
import platform.server.data.GlobalTable;
import platform.server.data.where.classes.ClassWhere;

public class DumbTable extends GlobalTable {

    public static final DumbTable instance = new DumbTable();

    public KeyField key;

    private DumbTable() {
        super("dumb");
        key = new KeyField("id", SystemClass.instance);
        keys.add(key);

        classes = new ClassWhere<KeyField>(key, SystemClass.instance);
    }

    @Override
    public int getCount() {
        throw new RuntimeException("not supported");
    }
}
