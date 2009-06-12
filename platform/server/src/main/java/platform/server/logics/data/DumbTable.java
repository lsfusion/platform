package platform.server.logics.data;

import platform.server.data.KeyField;
import platform.server.data.Table;
import platform.server.data.classes.SystemClass;
import platform.server.data.classes.where.ClassWhere;

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
