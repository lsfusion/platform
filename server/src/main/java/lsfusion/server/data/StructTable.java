package lsfusion.server.data;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.ByteArrayClass;
import lsfusion.server.data.expr.query.PropStat;
import lsfusion.server.data.query.stat.TableStatKeys;
import lsfusion.server.data.where.classes.ClassWhere;

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

    public TableStatKeys getTableStatKeys() {
        return getStatKeys(this, 1);
    }

    public ImMap<PropertyField,PropStat> getStatProps() {
        throw new RuntimeException("not supported");
    }
}
