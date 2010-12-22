package platform.server.logics.table;

import platform.server.classes.SystemClass;
import platform.server.data.KeyField;
import platform.server.data.Table;
import platform.server.data.GlobalTable;

public class EmptyTable extends GlobalTable {
    public static final EmptyTable instance = new EmptyTable();

    public KeyField key;

    public EmptyTable() {
        super("empty");
        keys.add(new KeyField("id", SystemClass.instance));
    }

}
