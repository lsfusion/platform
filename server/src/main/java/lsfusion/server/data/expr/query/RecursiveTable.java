package lsfusion.server.data.expr.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.query.exec.materialize.NotMaterializable;
import lsfusion.server.data.stat.PropStat;
import lsfusion.server.data.stat.TableStatKeys;
import lsfusion.server.data.table.Field;
import lsfusion.server.data.table.KeyField;
import lsfusion.server.data.table.NamedTable;
import lsfusion.server.data.table.PropertyField;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.classes.data.DataClass;

public class RecursiveTable extends NamedTable implements NotMaterializable {

    private final TableStatKeys statKeys;
    // assert'им что properties IntegralClass'ы
    
    public final boolean noInnerFollows;
    
    public RecursiveTable(String name, ImSet<KeyField> keys, ImSet<PropertyField> properties, ClassWhere<KeyField> classes, TableStatKeys statKeys, boolean noInnerFollows) {
        super(name, keys.sort(), properties, classes, getPropClasses(properties, classes));
        this.statKeys = statKeys;
        this.noInnerFollows = noInnerFollows;
    }

    public TableStatKeys getTableStatKeys() {
        return statKeys;
    }

    private static ImMap<PropertyField, ClassWhere<Field>> getPropClasses(ImSet<PropertyField> props, final ClassWhere<KeyField> keyClasses) {
        return props.mapValues((PropertyField prop) -> new ClassWhere<Field>(prop, (DataClass)prop.type).and(BaseUtils.<ClassWhere<Field>>immutableCast(keyClasses)));
    }

    @Override
    public boolean calcTwins(TwinImmutableObject o) {
        return super.calcTwins(o) && statKeys.equals(((RecursiveTable)o).statKeys) && noInnerFollows == ((RecursiveTable) o).noInnerFollows;
    }

    @Override
    public int immutableHashCode() {
        return 31 * (31 * super.immutableHashCode() + statKeys.hashCode()) + (noInnerFollows ? 1 : 0);
    }

    @IdentityLazy
    public ImMap<PropertyField, PropStat> getStatProps() { // assert что пустой если Logical рекурсия
        return getStatProps(this);
    }
}
