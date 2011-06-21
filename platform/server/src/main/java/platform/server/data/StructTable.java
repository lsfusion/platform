package platform.server.data;

import platform.server.classes.ByteArrayClass;
import platform.server.data.where.classes.ClassWhere;

public class StructTable extends GlobalTable {

    public final static StructTable instance = new StructTable();

    public PropertyField struct;

    public StructTable() {
        super("global");

        struct = new PropertyField("struct", ByteArrayClass.instance);
        properties.add(struct);

        classes = ClassWhere.TRUE();
        
        propertyClasses.put(struct,new ClassWhere<Field>(struct, ByteArrayClass.instance));
    }

    @Override
    public int getCount() {
        return 1;
    }
}
