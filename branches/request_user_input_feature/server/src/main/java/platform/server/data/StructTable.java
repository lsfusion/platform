package platform.server.data;

import platform.server.classes.ByteArrayClass;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.where.classes.ClassWhere;

import java.util.Map;

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

    public StatKeys<KeyField> getStatKeys() {
        return getStatKeys(this, 1);
    }

    public Map<PropertyField, Stat> getStatProps() {
        throw new RuntimeException("not supported");
    }
}
