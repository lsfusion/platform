package platform.server.data;

import platform.server.data.types.Type;

public class GlobalTable extends Table {

    public PropertyField struct;

    public GlobalTable() {
        super("global");

        struct = new PropertyField("struct", Type.bytes);
        properties.add(struct);
    }
}
