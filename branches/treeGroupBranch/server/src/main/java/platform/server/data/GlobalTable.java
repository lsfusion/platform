package platform.server.data;

import platform.server.classes.ByteArrayClass;
import platform.server.data.where.classes.ClassWhere;

public class GlobalTable extends Table {

    public final static GlobalTable instance = new GlobalTable();

    public PropertyField struct;

    public GlobalTable() {
        super("global");

        struct = new PropertyField("struct", ByteArrayClass.instance);
        properties.add(struct);

        classes = ClassWhere.TRUE();
        
        propertyClasses.put(struct,new ClassWhere<Field>(struct, ByteArrayClass.instance));
    }
}
