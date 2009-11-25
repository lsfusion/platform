package platform.server.logics.table;

import platform.server.data.KeyField;
import platform.server.data.Table;
import platform.server.data.where.classes.ClassWhere;
import platform.server.classes.SystemClass;

public class DumbTable extends Table {

    public static final DumbTable instance = new DumbTable();

    KeyField key;

    DumbTable() {
        super("dumb");
        key = new KeyField("id", SystemClass.instance);
        keys.add(key);

        classes = new ClassWhere<KeyField>(key, SystemClass.instance);
    }
}
