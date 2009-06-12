package platform.server.logics.data;

import platform.server.data.KeyField;
import platform.server.data.Table;
import platform.server.data.classes.SystemClass;

public class EmptyTable extends Table {
    public static final EmptyTable instance = new EmptyTable();

    KeyField key;

    EmptyTable() {
        super("empty");
        keys.add(new KeyField("id", SystemClass.instance));
    }

}
