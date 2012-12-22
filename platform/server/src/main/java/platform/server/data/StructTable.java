package platform.server.data;

import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.server.classes.ByteArrayClass;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.where.classes.ClassWhere;

public class StructTable extends GlobalTable {

    public final static StructTable instance = new StructTable();

    public PropertyField struct;

    public StructTable() {
        super("global");

        struct = new PropertyField("struct", ByteArrayClass.instance);
        properties = SetFact.singleton(struct);

        classes = ClassWhere.TRUE();
        
        propertyClasses = MapFact.singleton(struct, new ClassWhere<Field>(struct, ByteArrayClass.instance));
    }

    public StatKeys<KeyField> getStatKeys() {
        return getStatKeys(this, 1);
    }

    public ImMap<PropertyField, Stat> getStatProps() {
        throw new RuntimeException("not supported");
    }
}
